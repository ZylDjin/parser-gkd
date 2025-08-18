package com.company.parser.core.parsers;

import com.company.parser.core.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class DemidovPlaywrightParser implements SiteParser {

    private String baseUrl = "https://demidovsteel.ru/catalog/truby-profilnye/";

    public void setBaseUrl(String url) { this.baseUrl = url.endsWith("/") ? url : url + "/"; }

    @Override public Competitor competitor() { return Competitor.DEMIDOV; }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) throws Exception {
        List<PriceVariant> out = new ArrayList<>();

        // Рендерим категорию с догрузкой карточек
        String htmlCat = loadRenderedCategory(baseUrl, Path.of("debug","debug_demidov_category.html"));
        if (htmlCat != null) out.addAll(extractFromCategory(htmlCat, size));

        if (out.isEmpty()) {
            String htmlCat2 = loadRenderedCategory(baseUrl, Path.of("debug","debug_demidov_category_retry.html"));
            if (htmlCat2 != null) out.addAll(extractFromCategory(htmlCat2, size));
        }

        System.out.println("[DEMIDOV/PW] total variants matched: " + out.size());
        return out;
    }

    private String loadRenderedCategory(String url, Path dump) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 2400));
            Page page = ctx.newPage();
            page.setDefaultTimeout(30_000);

            System.out.println("[DEMIDOV/PW] GET " + url);
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // согласия/куки
            try { page.locator("button:has-text(\"Принять\"), button:has-text(\"Понятно\")").first().click(); } catch (Exception ignore) {}

            // проскроллим и прожмём "показать ещё"
            for (int i = 0; i < 10; i++) { page.mouse().wheel(0, 1600); page.waitForTimeout(400); }
            for (int i = 0; i < 6; i++) {
                var more = page.locator("button:has-text(\"Показать ещё\"), button:has-text(\"Показать еще\"), button:has-text(\"Еще\")");
                if (!more.isVisible()) break;
                try { more.click(); page.waitForTimeout(900); } catch (Exception ignore) { break; }
            }

            page.waitForTimeout(700); // дать дорисовать цены
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

    private List<PriceVariant> extractFromCategory(String html, SizeKey size) {
        Document doc = Jsoup.parse(html);
        List<PriceVariant> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // эвристики карточек
        var cards = doc.select("div.product-card, article.product-card, li.product-item, div.catalog-item, div.catalog__item");
        if (cards.isEmpty()) cards = doc.select("div,li,article");

        Pattern pSize = sizeRegex(size);

        for (Element card : cards) {
            Element titleEl = Optional.ofNullable(card.selectFirst(".product-card__title, .product-title, a, h3")).orElse(card);
            String title = normalize(titleEl.text());
            if (!pSize.matcher(title).find()) continue;

            BigDecimal price = tryCardPrice(card);
            if (price == null) price = tryNearRuble(card.text());
            if (price == null) price = tryJsonLd(card);

            if (price == null) continue;
            if (!isSane(price)) continue;

            if (!seen.add(price.toPlainString())) continue;

            boolean gost = title.toUpperCase(Locale.ROOT).contains("ГОСТ");
            boolean tu   = title.toUpperCase(Locale.ROOT).contains("ТУ");
            out.add(new PriceVariant(price, gost, tu, "pw"));
            System.out.println("[DEMIDOV/PW] MATCH size=" + size + " price=" + price);
            if (out.size() >= 6) break;
        }
        return out;
    }

    private static BigDecimal tryCardPrice(Element card) {
        String[] sels = {".product-card__price", ".price__current", ".product-price__current",
                "[itemprop=price]", "meta[itemprop=price]"};
        for (String s : sels) {
            Element pe = card.selectFirst(s);
            if (pe == null) continue;
            String raw = pe.hasAttr("content") ? pe.attr("content") : pe.text();
            BigDecimal v = parsePrice(raw);
            if (v != null) return v;
        }
        return null;
    }

    private static BigDecimal tryNearRuble(String text) {
        if (text != null && text.contains("₽")) return parsePrice(text);
        return null;
    }

    private static BigDecimal tryJsonLd(Element card) {
        for (Element s : card.select("script[type=application/ld+json]")) {
            BigDecimal v = parsePrice(s.data());
            if (v != null) return v;
        }
        return null;
    }

    private static boolean isSane(BigDecimal v) {
        int val = v.intValue();
        return (val >= 20000 && val <= 300000);
    }

    private static Pattern sizeRegex(SizeKey size) {
        String a = size.a().stripTrailingZeros().toPlainString();
        String b = size.b().stripTrailingZeros().toPlainString();
        String t = size.t().stripTrailingZeros().toPlainString();
        String x = "[x×Xх*]";
        String sep = "\\s*";
        String tAlt = t.contains(".") ? t.replace('.', ',') : t + "(?:[\\.,]0)?";
        String mm = "(?:\\s*мм)?";
        String p1 = a + sep + x + sep + b + sep + x + sep + tAlt + mm;
        String p2 = b + sep + x + sep + a + sep + x + sep + tAlt + mm;
        return Pattern.compile("(?i)(" + p1 + ")|(" + p2 + ")");
    }

    private static String normalize(String s) { return s == null ? "" : s.replace('\u00A0',' ').trim(); }

    private static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.isEmpty() || digits.length() > 8) return null;
        try { return new BigDecimal(digits); } catch (NumberFormatException e) { return null; }
    }
}
