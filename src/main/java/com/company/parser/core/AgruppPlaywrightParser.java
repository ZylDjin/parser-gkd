package com.company.parser.core;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AGRUPP (профильные трубы) через Playwright.
 * Улучшения:
 *  - Пробуем URL размера в двух вариантах: A×B×T и B×A×T (reverse slug).
 *  - На size-странице не фильтруем по containsSize(); на категории — фильтруем по обоим порядкам сторон.
 *  - containsSize допускает 2 | 2.0 | 2,0 и любые варианты "x" (x/X/×/х/*), опционально "мм".
 *  - PRICE_LABEL принимает "Цена/стоимость" (в т.ч. "от").
 *  - На size-странице fallback-извлечение цены по типовым селекторам ([itemprop=price], [data-price], [class*=price]).
 *  - Категория: автоскролл + клики "Показать ещё".
 *  - Дампы HTML в debug/.
 */
public class AgruppPlaywrightParser implements SiteParser {
    private final String baseUrl; // напр.: https://ag.market/catalog/truby-stalnye/truby-profilnye/

    public AgruppPlaywrightParser(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Override public Competitor competitor() { return Competitor.AGRUPP; }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) throws Exception {
        List<PriceVariant> out = new ArrayList<>();

        // 1) Пытаемся страницу размера в прямом порядке A×B×T
        String slugDirect = toSizeSlug(size);
        String urlDirect  = baseUrl + slugDirect + "/";
        System.out.println("[AGRUPP/PW] GET size-url: " + urlDirect);
        String htmlDirect = loadRenderedHtml(urlDirect, Path.of("debug", "debug_agrupp_size_" + slugDirect + ".html"));
        if (htmlDirect != null) {
            out.addAll(extractPriceVariantsFromHtml(htmlDirect, size, true));
        }

        // 2) Если ничего не нашли — пробуем переставленный порядок B×A×T (иногда так называют прямоугольник)
        if (out.isEmpty()) {
            String slugReverse = toReversedSizeSlug(size);
            String urlReverse  = baseUrl + slugReverse + "/";
            System.out.println("[AGRUPP/PW] GET size-url(reverse): " + urlReverse);
            String htmlReverse = loadRenderedHtml(urlReverse, Path.of("debug", "debug_agrupp_size_" + slugReverse + ".html"));
            if (htmlReverse != null) {
                out.addAll(extractPriceVariantsFromHtml(htmlReverse, size, true));
            }
        }

        // 3) Если всё ещё пусто — фолбэк на категорию (подгружаем карточки «Показать ещё»)
        if (out.isEmpty()) {
            System.out.println("[AGRUPP/PW] Fallback to category: " + baseUrl);
            String htmlCat = loadRenderedHtml(baseUrl, Path.of("debug", "debug_agrupp_category.html"));
            if (htmlCat != null) {
                out.addAll(extractPriceVariantsFromHtml(htmlCat, size, false));
            }
        }

        System.out.println("[AGRUPP/PW] total variants matched: " + out.size());
        return out;
    }

    // ---------- Playwright: рендер + дамп ----------

    private String loadRenderedHtml(String url, Path debugFile) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 2200));
            Page page = ctx.newPage();
            page.setDefaultTimeout(25_000);

            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            boolean isSizePage = url.matches(".*/\\d+x\\d+x[\\d-]+/?$");
            if (isSizePage) {
                // Лёгкий автоскролл
                for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 1600); page.waitForTimeout(400); }
            } else {
                // Категория: грузим всё, что можно
                loadAllCategoryItems(page);
            }

            String html = page.content();
            Files.createDirectories(debugFile.getParent());
            Files.writeString(debugFile, html, StandardCharsets.UTF_8);
            System.out.println("[AGRUPP/PW] saved " + debugFile.toAbsolutePath());
            browser.close();
            return html;
        } catch (Exception e) {
            System.out.println("[AGRUPP/PW] WARN loadRenderedHtml failed for " + url + " : " + e.getMessage());
            return null;
        }
    }

    private void loadAllCategoryItems(Page page) {
        // Немного скроллим…
        for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 1600); page.waitForTimeout(400); }
        // Пытаемся нажать «Показать ещё/еще/Ещё товары»
        for (int i = 0; i < 15; i++) {
            Locator more = page.locator("button:has-text(\"Показать ещё\"), button:has-text(\"Показать еще\"), button:has-text(\"Ещё товары\")");
            if (!more.isVisible()) break;
            try {
                more.click(new Locator.ClickOptions().setTimeout(2000));
                page.waitForTimeout(800);
            } catch (Exception ignore) {
                break;
            }
        }
        // Финальный скролл до низа
        for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 2000); page.waitForTimeout(300); }
    }

    // ---------- Извлечение цен ----------

    // Строго: "Цена/стоимость (от) <числа/пробелы> ₽"
    private static final Pattern PRICE_LABEL = Pattern.compile(
            "(?:Цена|стоимость)\\s*(?:от)?\\s*:?[\\s\\u00A0]*([\\d\\s\\u00A0]{2,})[\\s\\u00A0]*₽",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private List<PriceVariant> extractPriceVariantsFromHtml(String html, SizeKey size, boolean isSizePage) {
        Document doc = Jsoup.parse(html);
        List<PriceVariant> out = new ArrayList<>();

        record Key(BigDecimal p, boolean gost, boolean tu) {}
        Set<Key> seen = new HashSet<>();

        // После изучения debug-файлов можно сузить до конкретных карточек (article.product-card и т.п.)
        Elements blocks = doc.select("article, li, div, section");
        int added = 0;

        for (Element block : blocks) {
            if (added >= 8) break;

            String text = normalize(block.text());
            if (text.isBlank()) continue;

            // На size-странице не фильтруем по размеру. На категории — фильтруем по ОБОИМ порядкам.
            if (!isSizePage && !containsSizeEitherOrder(text, size)) continue;

            // Сначала строгая метка "Цена: … ₽"
            String priceText = extractPriceByLabel(block);

            // Если не нашли — на size-странице пробуем аккуратный CSS-fallback (чтобы не ловить мусор с категории)
            if (priceText == null && isSizePage) {
                priceText = extractPriceByCssFallback(block);
            }
            if (priceText == null) continue;

            BigDecimal price = parsePrice(priceText);
            if (price == null) continue;

            String up = text.toUpperCase(Locale.ROOT);
            boolean gost = up.contains("ГОСТ");
            boolean tu   = up.contains("ТУ");

            Key k = new Key(price, gost, tu);
            if (!seen.add(k)) continue;

            out.add(new PriceVariant(price, gost, tu, "pw"));
            added++;
            if (added <= 5) {
                System.out.println("[AGRUPP/PW] MATCH size=" + size + " price=" + price +
                        " mark=" + (gost ? "ГОСТ" : (tu ? "ТУ" : "-")));
            }
        }
        return out;
    }

    private static String extractPriceByLabel(Element block) {
        Matcher m = PRICE_LABEL.matcher(block.text());
        if (m.find()) return m.group(1);
        m = PRICE_LABEL.matcher(block.html());
        if (m.find()) return m.group(1);
        return null;
    }

    // Осторожный fallback ТОЛЬКО для size-страницы
    private static String extractPriceByCssFallback(Element block) {
        // приоритет: структурированные атрибуты
        Elements cand = block.select("[itemprop=price], [data-price], [data-product-price]");
        for (Element e : cand) {
            String t = normalize(e.text());
            if (looksLikePrice(t)) return t;
            String v = e.attr("content");
            if (looksLikePrice(v)) return v;
            v = e.attr("value");
            if (looksLikePrice(v)) return v;
        }
        // затем любые классы, содержащие "price"
        cand = block.select("*[class*=price]");
        for (Element e : cand) {
            String t = normalize(e.text());
            if (looksLikePrice(t)) return t;
        }
        return null;
    }

    // ---------- Утилиты ----------

    /** 40x20x1.5 -> 40x20x1-5 */
    private static String toSizeSlug(SizeKey s) {
        String a = s.a().stripTrailingZeros().toPlainString();
        String b = s.b().stripTrailingZeros().toPlainString();
        String t = s.t().stripTrailingZeros().toPlainString().replace('.', '-');
        return a + "x" + b + "x" + t;
    }
    /** reverse: 40x20x1.5 -> 20x40x1-5 */
    private static String toReversedSizeSlug(SizeKey s) {
        String a = s.a().stripTrailingZeros().toPlainString();
        String b = s.b().stripTrailingZeros().toPlainString();
        String t = s.t().stripTrailingZeros().toPlainString().replace('.', '-');
        return b + "x" + a + "x" + t;
    }

    private static String normalize(String s) { return s.replace('\u00A0', ' ').trim(); }

    // допускаем "₽" и/или 4+ цифры
    private static boolean looksLikePrice(String s) {
        if (s == null) return false;
        String t = normalize(s);
        if (t.contains("₽")) return true;
        return t.replaceAll("[^\\d]", "").length() >= 4;
    }

    /** Проверка троицы в тексте для ОБОИХ порядков сторон + толщина 2 | 2.0 | 2,0 + любые "x" + опционально "мм" */
    private static boolean containsSizeEitherOrder(String text, SizeKey size) {
        String a = size.a().stripTrailingZeros().toPlainString();
        String b = size.b().stripTrailingZeros().toPlainString();
        String tBase = size.t().stripTrailingZeros().toPlainString();

        String tPattern;
        if (tBase.contains(".")) {
            String tComma = tBase.replace('.', ',');
            tPattern = "(?:" + Pattern.quote(tBase) + "|" + Pattern.quote(tComma) + ")";
        } else {
            tPattern = "(?:" + Pattern.quote(tBase) + "|" + Pattern.quote(tBase + ".0") + "|" + Pattern.quote(tBase + ",0") + ")";
        }

        String x = "[x×Xх*]";                  // любые разделители «x»
        String mm = "(?:\\s*мм)?";             // опционально «мм»
        String sep = "\\s*";                   // гибкие пробелы

        String pair1 = Pattern.quote(a) + sep + x + sep + Pattern.quote(b);
        String pair2 = Pattern.quote(b) + sep + x + sep + Pattern.quote(a);

        String regex = "(?is).*(" + pair1 + "|" + pair2 + ")" + sep + x + sep + tPattern + mm + ".*";

        String norm = text.replace('\u00A0',' ');
        return norm.matches(regex);
    }

    private static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.isEmpty() || digits.length() > 7) return null; // отсечь «простыни»
        try { return new BigDecimal(digits); }
        catch (NumberFormatException e) { return null; }
    }
}
