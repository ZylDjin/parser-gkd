package com.company.parser.core;

import com.company.parser.core.parsers.SiteParser;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class DemidovPlaywrightParser implements SiteParser {
    private final String baseUrl; // https://demidovsteel.ru/catalog/truby-profilnye/

    public DemidovPlaywrightParser(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Override public Competitor competitor() { return Competitor.DEMIDOV; }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) throws Exception {
        List<PriceVariant> out = new ArrayList<>();

        // 1) пробуем страницу размера (если slug работает)
        String slug = toSizeSlug(size); // 40x20x1.5 -> 40x20x1-5
        String sizeUrl = baseUrl + slug + "/";
        String htmlSize = loadRenderedHtml(sizeUrl, Path.of("debug", "debug_demidov_size_" + slug + ".html"), true);
        if (htmlSize != null) out.addAll(extract(htmlSize, size, true));

        // 2) fallback на категорию
        if (out.isEmpty()) {
            String htmlCat = loadRenderedHtml(baseUrl, Path.of("debug", "debug_demidov_category.html"), false);
            if (htmlCat != null) out.addAll(extract(htmlCat, size, false));
        }

        System.out.println("[DEMIDOV/PW] total variants matched: " + out.size());
        return out;
    }

    private String loadRenderedHtml(String url, Path dump, boolean sizePage) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 2200));
            Page page = ctx.newPage();
            page.setDefaultTimeout(25_000);

            System.out.println("[DEMIDOV/PW] GET " + url);
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            if (sizePage) {
                for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 1600); page.waitForTimeout(400); }
            } else {
                // категория: дорисуем «показать ещё», если есть
                for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 1600); page.waitForTimeout(400); }
                for (int i = 0; i < 15; i++) {
                    Locator more = page.locator("button:has-text(\"Показать ещё\"), button:has-text(\"Показать еще\"), button:has-text(\"Ещё товары\")");
                    if (!more.isVisible()) break;
                    try { more.click(); page.waitForTimeout(800); } catch (Exception ignore) { break; }
                }
            }

            String html = page.content();
            Files.createDirectories(dump.getParent());
            Files.writeString(dump, html, StandardCharsets.UTF_8);
            System.out.println("[DEMIDOV/PW] saved " + dump.toAbsolutePath());
            browser.close();
            return html;
        } catch (Exception e) {
            System.out.println("[DEMIDOV/PW] WARN " + e.getMessage());
            return null;
        }
    }

    private List<PriceVariant> extract(String html, SizeKey size, boolean isSizePage) {
        Document doc = Jsoup.parse(html);
        List<PriceVariant> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // широкая выборка блоков — потом сузим, если дадите HTML
        var blocks = doc.select("article, li, div, section");
        int added = 0;

        for (Element b : blocks) {
            if (added >= 6) break;

            String txt = normalize(b.text());
            if (txt.isBlank()) continue;

            if (!isSizePage && !containsSizeEitherOrder(txt, size)) continue;

            // 1) сначала ищем «₽» — как у AGRUPP
            String price = extractRubleLike(b);
            if (price == null && isSizePage) {
                // 2) на size-странице пробуем атрибуты
                price = extractPriceAttr(b);
            }
            if (price == null) continue;

            BigDecimal val = parsePrice(price);
            if (val == null) continue;

            String key = val.toPlainString();
            if (!seen.add(key)) continue;

            boolean gost = txt.toUpperCase(Locale.ROOT).contains("ГОСТ");
            boolean tu   = txt.toUpperCase(Locale.ROOT).contains("ТУ");

            out.add(new PriceVariant(val, gost, tu, "pw"));
            System.out.println("[DEMIDOV/PW] MATCH size=" + size + " price=" + val);
            added++;
        }
        return out;
    }

    private static String extractRubleLike(Element b) {
        // ищем куски с символом ₽ либо цифры с пробелами
        String t = normalize(b.text());
        String digits = t.replaceAll("[^\\d]", "");
        if (t.contains("₽") && digits.length() >= 4) return t;
        // иногда цена в дочерних span с классом price
        for (Element e : b.select("*[class*=price], [itemprop=price]")) {
            String s = normalize(e.text());
            if (s.contains("₽") || s.replaceAll("[^\\d]","").length() >= 4) return s;
        }
        return null;
    }

    private static String extractPriceAttr(Element b) {
        for (Element e : b.select("[itemprop=price], [data-price], [data-product-price]")) {
            String v = e.hasAttr("content") ? e.attr("content") : e.attr("value");
            if (v == null || v.isBlank()) v = e.text();
            if (v != null && v.replaceAll("[^\\d]","").length() >= 4) return v;
        }
        return null;
    }

    // ==== utils ====
    private static String normalize(String s) { return s.replace('\u00A0', ' ').trim(); }

    private static String toSizeSlug(SizeKey s) {
        String a = s.a().stripTrailingZeros().toPlainString();
        String b = s.b().stripTrailingZeros().toPlainString();
        String t = s.t().stripTrailingZeros().toPlainString().replace('.', '-');
        return a + "x" + b + "x" + t;
    }

    private static boolean containsSizeEitherOrder(String text, SizeKey size) {
        String a = size.a().stripTrailingZeros().toPlainString();
        String b = size.b().stripTrailingZeros().toPlainString();
        String tBase = size.t().stripTrailingZeros().toPlainString();

        String tPattern = tBase.contains(".")
                ? "(?:" + Pattern.quote(tBase) + "|" + Pattern.quote(tBase.replace('.', ',')) + ")"
                : "(?:" + Pattern.quote(tBase) + "|" + Pattern.quote(tBase + ".0") + "|" + Pattern.quote(tBase + ",0") + ")";

        String x = "[x×Xх*]";
        String mm = "(?:\\s*мм)?";
        String sep = "\\s*";
        String p1 = Pattern.quote(a) + sep + x + sep + Pattern.quote(b);
        String p2 = Pattern.quote(b) + sep + x + sep + Pattern.quote(a);

        String regex = "(?is).*(" + p1 + "|" + p2 + ")" + sep + x + sep + tPattern + mm + ".*";
        return text.replace('\u00A0',' ').matches(regex);
    }

    private static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.isEmpty() || digits.length() > 7) return null;
        try { return new BigDecimal(digits); } catch (NumberFormatException e) { return null; }
    }
}
