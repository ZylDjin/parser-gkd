package com.company.parser.core.parsers;

import com.company.parser.config.SizesConfig;
import com.company.parser.config.AppProperties;
import com.company.parser.core.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DemidovPlaywrightParser implements SiteParser {

    private static final Logger log = LoggerFactory.getLogger(DemidovPlaywrightParser.class);

    private final AppProperties props;     // для границ цен и debugDir
    private final SizesConfig sizesConfig;

    public DemidovPlaywrightParser(AppProperties props, SizesConfig sizesConfig) {
        this.props = props;
        this.sizesConfig = sizesConfig;
    }

    @Override
    public Competitor competitor() {
        return Competitor.DEMIDOV;
    }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) throws Exception {
        String baseUrl = sizesConfig.baseUrl(category, competitor());
        if (baseUrl == null) baseUrl = props.getCompetitorsBaseUrls().get(competitor());
        if (baseUrl == null) throw new IllegalStateException("Base URL for DEMIDOV is not configured");

        // Демидов использует точку в толщине: 25x25x1.5
        String sizeSlug = size.width() + "x" + size.height() + "x" + size.thicknessDot(); // 40x20x1.5
        String sizeUrl = joinUrl(baseUrl, sizeSlug) + "/";

        List<PriceVariant> out = new ArrayList<>();

        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext();
            Page page = ctx.newPage();

            // 1) Пытаемся на странице конкретного размера
            log.info("[DEMIDOV/PW] GET {}", sizeUrl);
            page.navigate(sizeUrl);
            // ждём, что прогрузится хоть что-то осмысленное
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(400); // небольшой буфер
            save(page, "debug_demidov_size_" + sizeSlug.replace('.', '-'));

            List<BigDecimal> nearPrices = extractPricesNearSize(page.content(), size);
            log.info("[DEMIDOV/PW] near-window matches (size page): {}", nearPrices.size());
            for (BigDecimal p : nearPrices) out.add(new PriceVariant(p, "-"));

            // 2) Если не нашли на size-странице — идём в категорию и ищем «рядом» с размером
            if (out.isEmpty()) {
                log.info("[DEMIDOV/PW] GET {}", baseUrl);
                page.navigate(baseUrl);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                page.waitForTimeout(600);
                save(page, "debug_demidov_category");

                List<BigDecimal> nearCategory = extractPricesNearSize(page.content(), size);
                log.info("[DEMIDOV/PW] near-window matches (category): {}", nearCategory.size());
                for (BigDecimal p : nearCategory) out.add(new PriceVariant(p, "-"));
            }

            page.close();
            ctx.close();
            browser.close();
        }

        log.info("[DEMIDOV/PW] total variants matched: {}", out.size());
        return out;
    }

    // --- утилиты ---

    private void save(Page page, String fileNameNoExt) {
        try {
            Path dir = Path.of(props.getDebugDir());
            dir.toFile().mkdirs();
            String p = dir.resolve(fileNameNoExt + ".html").toString();
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(fileNameNoExt + ".png")));
            java.nio.file.Files.writeString(dir.resolve(fileNameNoExt + ".html"), page.content());
            log.info("[DEMIDOV/PW] saved {}", p);
        } catch (Exception ignore) {
        }
    }

    private static String joinUrl(String base, String slug) {
        if (!base.endsWith("/")) base = base + "/";
        return base + slug;
    }

    /**
     * Ищем цену только в «окне» вокруг упоминания размера.
     * Допускаем варианты записи: x/×, пробелы, запятая/точка в толщине.
     */
    private static List<BigDecimal> extractPricesNearSize(String html, SizeKey size) {
        String sizeRegex = sizePattern(size); // толерантный паттерн для размера
        Pattern needle = Pattern.compile(sizeRegex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        List<int[]> windows = new ArrayList<>();
        Matcher m = needle.matcher(html);
        while (m.find()) {
            int start = Math.max(0, m.start() - 800);
            int end = Math.min(html.length(), m.end() + 800);
            windows.add(new int[]{start, end});
        }

        // если упоминаний размера нет — нечего пытаться
        if (windows.isEmpty()) return List.of();

        Pattern priceRe = Pattern.compile(
                "(?<!\\d)(\\d{2}\\s?\\d{3}|\\d{5,6})(?=\\s*(?:₽|руб))",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        List<BigDecimal> found = new ArrayList<>();
        for (int[] w : windows) {
            String chunk = html.substring(w[0], w[1]);
            Matcher pm = priceRe.matcher(chunk);
            while (pm.find()) {
                String raw = pm.group(1).replace(" ", "");
                try {
                    BigDecimal val = new BigDecimal(raw);
                    found.add(val);
                } catch (NumberFormatException ignored) {}
            }
        }
        return found;
    }

    private static String sizePattern(SizeKey s) {
        // Пример: 40x20x1.5  или 40 × 20 × 1,5  — допускаем x/×, пробелы, точка/запятая
        String n1 = String.valueOf(s.width());
        String n2 = String.valueOf(s.height());
        String tDot = s.thicknessDot();      // "1.5"
        String tComma = tDot.replace('.', ','); // "1,5"
        String x = "[x×]"; // символ умножения
        String sp = "\\s*";
        return n1 + sp + x + sp + n2 + sp + x + sp + "(" + Pattern.quote(tDot) + "|" + Pattern.quote(tComma) + ")";
    }
}
