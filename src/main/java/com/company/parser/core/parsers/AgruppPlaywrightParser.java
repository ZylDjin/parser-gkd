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
public class AgruppPlaywrightParser implements SiteParser {

    private String baseUrl = "https://ag.market/catalog/truby-stalnye/truby-profilnye/";

    public void setBaseUrl(String url) {
        this.baseUrl = url.endsWith("/") ? url : url + "/";
    }

    @Override public Competitor competitor() { return Competitor.AGRUPP; }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) throws Exception {
        List<PriceVariant> out = new ArrayList<>();

        String slug = toSizeSlug(size);
        String sizeUrl = baseUrl + slug + "/";
        String htmlSize = loadRenderedHtml(sizeUrl, Path.of("debug","debug_agrupp_size_" + slug + ".html"), true);
        if (htmlSize != null) out.addAll(extract(htmlSize, size, true));

        if (out.isEmpty()) {
            String htmlCat = loadRenderedHtml(baseUrl, Path.of("debug","debug_agrupp_category.html"), false);
            if (htmlCat != null) out.addAll(extract(htmlCat, size, false));
        }

        System.out.println("[AGRUPP/PW] total variants matched: " + out.size());
        return out;
    }

    private String loadRenderedHtml(String url, Path dump, boolean sizePage) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 2200));
            Page page = ctx.newPage();
            page.setDefaultTimeout(25_000);

            System.out.println("[AGRUPP/PW] GET " + url);
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            for (int i = 0; i < 6; i++) { page.mouse().wheel(0, 1500); page.waitForTimeout(350); }
            // показать ещё, если есть
            for (int i = 0; i < 10; i++) {
                Locator more = page.locator("button:has-text(\"Показать ещё\"), button:has-text(\"Показать еще\"), button:has-text(\"Еще\")");
                if (!more.isVisible()) break;
                try { more.click(); page.waitForTimeout(800); } catch (Exception ignore) { break; }
            }

            String html = page.content();
            Files.createDirectories(dump.getParent());
            Files.writeString(dump, html, StandardCharsets.UTF_8);
            System.out.println("[AGRUPP/PW] saved " + dump.toAbsolutePath());
            browser.close();
            return html;
        } catch (Exception e) {
            System.out.println("[AGRUPP/PW] WARN " + e.getMessage());
            return null;
        }
    }

    private List<PriceVariant> extract(String html, SizeKey size, boolean isSizePage) {
        Document doc = Jsoup.parse(html);
        List<PriceVariant> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        var candidates = doc.select("article, li, div.catalog-item, div.product-card, div");
        Pattern pSize = sizeRegex(size);

        for (Element el : candidates) {
            String text = el.text();
            if (text == null || text.isBlank()) continue;
            if (!isSizePage && !pSize.matcher(text).find()) continue;

            BigDecimal price = extractPrice(el);
            if (price == null) continue;

            String key = price.toPlainString();
            if (!seen.add(key)) continue;

            boolean gost = text.toUpperCase(Locale.ROOT).contains("ГОСТ");
            boolean tu   = text.toUpperCase(Locale.ROOT).contains("ТУ");

            out.add(new PriceVariant(price, gost, tu, "pw"));
            if (out.size() >= 6) break;
        }
        return out;
    }

    private static BigDecimal extractPrice(Element el) {
        // частые селекторы цен
        String[] sels = {".price__current",".product-price__current",".product-card__price","[itemprop=price]","meta[itemprop=price]"};
        for (String s : sels) {
            var node = el.selectFirst(s);
            if (node == null) continue;
            String raw = node.hasAttr("content") ? node.attr("content") : node.text();
            BigDecimal v = parsePrice(raw);
            if (v != null) return v;
        }
        String txt = el.text();
        if (txt.contains("₽")) return parsePrice(txt);
        return null;
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

    private static String toSizeSlug(SizeKey s) {
        String a = s.a().stripTrailingZeros().toPlainString();
        String b = s.b().stripTrailingZeros().toPlainString();
        String t = s.t().stripTrailingZeros().toPlainString().replace('.', '-');
        return a + "x" + b + "x" + t;
    }

    private static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.isEmpty()) return null;
        try { return new BigDecimal(digits); } catch (NumberFormatException e) { return null; }
    }
}
