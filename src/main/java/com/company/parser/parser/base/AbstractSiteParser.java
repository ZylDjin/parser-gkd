package com.company.parser.parser.base;

import com.company.parser.client.HttpClient;
import com.company.parser.exception.ParseException;
import com.company.parser.exception.ScrapingException;
import com.company.parser.model.domain.ParsingConfig;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import com.company.parser.util.SizeNormalizer;
import com.company.parser.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Базовый абстрактный парсер для всех сайтов
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSiteParser implements SiteParser {

    protected final HttpClient httpClient;
    protected final TextUtils textUtils;
    protected final SizeNormalizer sizeNormalizer;

    /**
     * Основной метод парсинга сайта
     */
    @Override
    public List<PriceRecord> parse(Site site) {
        validateSite(site);

        log.info("Starting parse for site: {} ({})", site.getId(), site.getName());

        try {
            List<PriceRecord> records = new ArrayList<>();
            Set<String> visitedUrls = new HashSet<>();
            ParsingContext context = createContext(site);

            String currentUrl = site.getStartUrl();
            int pageNumber = 1;

            while (shouldContinueParsing(context, pageNumber, records.size(), currentUrl)) {
                // Проверка на циклы
                if (!visitedUrls.add(currentUrl)) {
                    log.warn("URL already visited: {}", currentUrl);
                    break;
                }

                try {
                    // Загрузка страницы
                    Document document = loadPage(currentUrl);

                    // Парсинг товаров на странице
                    List<PriceRecord> pageRecords = parsePage(document, site, context, currentUrl);

                    if (pageRecords.isEmpty() && pageNumber > 1) {
                        log.info("No items found on page {}, stopping", pageNumber);
                        break;
                    }

                    records.addAll(pageRecords);
                    log.info("Page {} parsed: {} items found (total: {})",
                            pageNumber, pageRecords.size(), records.size());

                    // Получение URL следующей страницы
                    currentUrl = getNextPageUrl(document, site);
                    pageNumber++;

                    // Задержка между страницами
                    if (currentUrl != null) {
                        pauseBetweenPages(site.getConfig());
                    }

                } catch (Exception e) {
                    log.error("Error parsing page {} of site {}: {}",
                            pageNumber, site.getId(), e.getMessage());

                    if (pageNumber == 1) {
                        // Если первая страница не загрузилась, прерываем
                        throw new ScrapingException(site.getId(), currentUrl,
                                "Failed to load first page: " + e.getMessage());
                    }
                    // Для остальных страниц продолжаем
                }
            }

            // Пост-обработка результатов
            List<PriceRecord> processedRecords = postProcess(records, site);

            log.info("Parsing completed for site {}: {} records", site.getId(), processedRecords.size());
            return processedRecords;

        } catch (Exception e) {
            log.error("Failed to parse site {}: {}", site.getId(), e.getMessage(), e);
            throw new ScrapingException(site.getId(), "Parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Парсинг одной страницы
     */
    protected abstract List<PriceRecord> parsePage(Document document, Site site,
                                                   ParsingContext context, String pageUrl);

    /**
     * Получение URL следующей страницы
     */
    protected abstract String getNextPageUrl(Document document, Site site);

    /**
     * Создание контекста парсинга
     */
    protected ParsingContext createContext(Site site) {
        return new ParsingContext(site);
    }

    /**
     * Валидация конфигурации сайта
     */
    protected void validateSite(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }
        if (site.getStartUrl() == null || site.getStartUrl().isBlank()) {
            throw new IllegalArgumentException("Start URL is required for site: " + site.getId());
        }
        if (site.getSelectors() == null) {
            throw new IllegalArgumentException("Selectors are required for site: " + site.getId());
        }
    }

    /**
     * Загрузка страницы
     */
    protected Document loadPage(String url) throws Exception {
        log.debug("Loading page: {}", url);
        return httpClient.get(url);
    }

    /**
     * Проверка необходимости продолжения парсинга
     */
    protected boolean shouldContinueParsing(ParsingContext context, int pageNumber,
                                            int recordsCount, String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        ParsingConfig config = context.getSite().getConfig();
        if (config == null) {
            config = ParsingConfig.builder().build();
        }

        boolean continueByPages = pageNumber <= config.getMaxPages();
        boolean continueByRecords = recordsCount < config.getMaxRecords();

        return continueByPages && continueByRecords;
    }

    /**
     * Пауза между загрузкой страниц
     */
    protected void pauseBetweenPages(ParsingConfig config) {
        if (config == null) return;

        long delay = config.getDelayBetweenPages();
        if (delay > 0) {
            // Добавляем случайность ±25%
            long randomDelay = (long) (delay * (0.75 + ThreadLocalRandom.current().nextDouble() * 0.5));

            try {
                log.debug("Waiting {}ms before next page", randomDelay);
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Пост-обработка результатов
     */
    protected List<PriceRecord> postProcess(List<PriceRecord> records, Site site) {
        return records.stream()
                .filter(this::isValidRecord)
                .map(record -> enrichRecord(record, site))
                .distinct()
                .toList();
    }

    /**
     * Проверка валидности записи
     */
    protected boolean isValidRecord(PriceRecord record) {
        if (record == null) return false;
        if (record.getProductName() == null || record.getProductName().isBlank()) return false;
        if (record.getPrice() == null || record.getPrice().compareTo(BigDecimal.ZERO) <= 0) return false;

        return true;
    }

    /**
     * Обогащение записи дополнительными данными
     */
    protected PriceRecord enrichRecord(PriceRecord record, Site site) {
        // Установка значений по умолчанию из конфигурации сайта
        if (record.getCurrency() == null || record.getCurrency().isBlank()) {
            record.setCurrency(site.getDefaultCurrency());
        }
        if (record.getUnit() == null || record.getUnit().isBlank()) {
            record.setUnit(site.getDefaultUnit());
        }
        if (record.getSiteId() == null || record.getSiteId().isBlank()) {
            record.setSiteId(site.getId());
        }
        if (record.getSiteName() == null || record.getSiteName().isBlank()) {
            record.setSiteName(site.getName());
        }
        if (record.getTimestamp() == null) {
            record.setTimestamp(Instant.now());
        }

        // Нормализация размеров
        if (record.getSize() != null) {
            record.setSize(sizeNormalizer.normalizeSize(record.getSize()));
        }
        if (record.getThickness() != null) {
            record.setThickness(sizeNormalizer.normalizeThickness(record.getThickness()));
        }

        return record;
    }

    /**
     * Безопасное извлечение текста по селектору
     */
    protected String extractText(Element element, String selector) {
        if (element == null || selector == null || selector.isBlank()) {
            return "";
        }

        try {
            Element found = element.selectFirst(selector);
            return found != null ? textUtils.normalize(found.text()) : "";
        } catch (Exception e) {
            log.debug("Failed to extract text with selector '{}': {}", selector, e.getMessage());
            return "";
        }
    }

    /**
     * Безопасное извлечение цены
     */
    protected BigDecimal extractPrice(Element element, String selector) {
        String text = extractText(element, selector);
        return textUtils.extractPrice(text).orElse(null);
    }

    /**
     * Безопасное извлечение URL
     */
    protected String extractUrl(Element element, String selector) {
        if (element == null || selector == null || selector.isBlank()) {
            return null;
        }

        try {
            Element link = element.selectFirst(selector);
            return link != null ? link.absUrl("href") : null;
        } catch (Exception e) {
            log.debug("Failed to extract URL with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }
}
