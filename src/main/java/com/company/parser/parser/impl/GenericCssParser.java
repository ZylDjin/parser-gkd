package com.company.parser.parser.impl;

import com.company.parser.client.HttpClient;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import com.company.parser.model.domain.SiteSelectors;
import com.company.parser.parser.base.AbstractSiteParser;
import com.company.parser.parser.base.ParsingContext;
import com.company.parser.util.SizeNormalizer;
import com.company.parser.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Универсальный CSS парсер для большинства сайтов
 */
@Slf4j
@Component("genericCssParser")
public class GenericCssParser extends AbstractSiteParser {

    @Autowired
    public GenericCssParser(HttpClient httpClient, TextUtils textUtils, SizeNormalizer sizeNormalizer) {
        super(httpClient, textUtils, sizeNormalizer);
    }

    @Override
    public String getType() {
        return "GENERIC";
    }

    @Override
    public boolean supports(Site site) {
        return "GENERIC".equalsIgnoreCase(site.getParserType()) ||
                site.getParserType() == null;
    }

    @Override
    protected List<PriceRecord> parsePage(Document document, Site site,
                                          ParsingContext context, String pageUrl) {
        List<PriceRecord> records = new ArrayList<>();
        SiteSelectors selectors = site.getSelectors();

        // Поиск карточек товаров
        Elements productCards = document.select(selectors.getListSelector());
        log.debug("Found {} product cards on page", productCards.size());

        for (Element card : productCards) {
            try {
                PriceRecord record = parseProductCard(card, site, pageUrl);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.debug("Failed to parse product card: {}", e.getMessage());
            }
        }

        context.addRecords(records.size());
        return records;
    }

    @Override
    protected String getNextPageUrl(Document document, Site site) {
        String nextPageSelector = site.getSelectors().getNextPageSelector();

        if (nextPageSelector == null || nextPageSelector.isBlank()) {
            return null;
        }

        // Пробуем разные варианты селекторов
        String[] selectors = nextPageSelector.split("\\|");

        for (String selector : selectors) {
            Element nextLink = document.selectFirst(selector.trim());
            if (nextLink != null) {
                String nextUrl = nextLink.absUrl("href");
                if (!nextUrl.isBlank()) {
                    log.debug("Found next page URL: {}", nextUrl);
                    return nextUrl;
                }
            }
        }

        return null;
    }

    /**
     * Парсинг карточки товара
     */
    private PriceRecord parseProductCard(Element card, Site site, String pageUrl) {
        SiteSelectors selectors = site.getSelectors();

        // Извлечение основных данных
        String name = extractText(card, selectors.getNameSelector());
        if (name.isBlank()) {
            log.debug("Product name is empty, skipping");
            return null;
        }

        // Извлечение цены
        BigDecimal price = extractPrice(card, selectors.getPriceSelector());
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Invalid price for product: {}", name);
            return null;
        }

        // URL товара
        String productUrl = extractUrl(card, selectors.getUrlSelector());
        if (productUrl == null || productUrl.isBlank()) {
            productUrl = pageUrl;
        }

        // Извлечение размеров и характеристик
        ProductDimensions dimensions = extractDimensions(card, selectors, name);

        // Если не удалось извлечь размеры с карточки и включена загрузка деталей
        if (shouldFetchDetails(site, dimensions)) {
            dimensions = fetchProductDetails(productUrl, selectors, dimensions);
        }

        // Создание записи
        return PriceRecord.builder()
                .productName(name)
                .size(dimensions.size)
                .thickness(dimensions.thickness)
                .steelGrade(dimensions.steelGrade)
                .length(dimensions.length)
                .price(price)
                .url(productUrl)
                .currency(extractCurrency(card))
                .unit(extractUnit(card))
                .build();
    }

    /**
     * Извлечение размеров и характеристик
     */
    private ProductDimensions extractDimensions(Element element, SiteSelectors selectors, String productName) {
        ProductDimensions dimensions = new ProductDimensions();

        // Извлечение из названия товара
        dimensions.size = textUtils.extractSize(productName).orElse("");
        dimensions.thickness = textUtils.extractThickness(productName).orElse("");
        dimensions.steelGrade = textUtils.extractSteelGrade(productName).orElse("");
        dimensions.length = textUtils.extractLength(productName).orElse("");

        // Извлечение из блока с размерами
        String sizeBlock = extractText(element, selectors.getSizeSelector());
        if (!sizeBlock.isBlank()) {
            if (dimensions.size.isBlank()) {
                dimensions.size = textUtils.extractSize(sizeBlock).orElse(dimensions.size);
            }
            if (dimensions.thickness.isBlank()) {
                dimensions.thickness = textUtils.extractThickness(sizeBlock).orElse(dimensions.thickness);
            }
            if (dimensions.steelGrade.isBlank()) {
                dimensions.steelGrade = textUtils.extractSteelGrade(sizeBlock).orElse(dimensions.steelGrade);
            }
            if (dimensions.length.isBlank()) {
                dimensions.length = textUtils.extractLength(sizeBlock).orElse(dimensions.length);
            }
        }

        // Дополнительные селекторы если указаны
        if (selectors.getThicknessSelector() != null) {
            String thickness = extractText(element, selectors.getThicknessSelector());
            if (!thickness.isBlank()) {
                dimensions.thickness = thickness;
            }
        }

        if (selectors.getSteelGradeSelector() != null) {
            String steelGrade = extractText(element, selectors.getSteelGradeSelector());
            if (!steelGrade.isBlank()) {
                dimensions.steelGrade = steelGrade;
            }
        }

        if (selectors.getLengthSelector() != null) {
            String length = extractText(element, selectors.getLengthSelector());
            if (!length.isBlank()) {
                dimensions.length = length;
            }
        }

        return dimensions;
    }

    /**
     * Проверка необходимости загрузки детальной страницы
     */
    private boolean shouldFetchDetails(Site site, ProductDimensions dimensions) {
        if (site.getConfig() == null || !site.getConfig().isFetchProductDetails()) {
            return false;
        }

        // Загружаем детали если не хватает важных характеристик
        return dimensions.size.isBlank() || dimensions.thickness.isBlank();
    }

    /**
     * Загрузка детальной информации о товаре
     */
    private ProductDimensions fetchProductDetails(String url, SiteSelectors selectors,
                                                  ProductDimensions current) {
        if (url == null || url.isBlank()) {
            return current;
        }

        try {
            log.debug("Fetching product details from: {}", url);
            Document doc = httpClient.get(url);

            // Извлечение из всего текста страницы
            String fullText = doc.text();
            ProductDimensions details = new ProductDimensions();

            details.size = textUtils.extractSize(fullText).orElse(current.size);
            details.thickness = textUtils.extractThickness(fullText).orElse(current.thickness);
            details.steelGrade = textUtils.extractSteelGrade(fullText).orElse(current.steelGrade);
            details.length = textUtils.extractLength(fullText).orElse(current.length);

            // Если указан селектор для блока с характеристиками
            if (selectors.getSizeSelector() != null) {
                String sizeBlock = extractText(doc.body(), selectors.getSizeSelector());
                if (!sizeBlock.isBlank()) {
                    details.size = textUtils.extractSize(sizeBlock).orElse(details.size);
                    details.thickness = textUtils.extractThickness(sizeBlock).orElse(details.thickness);
                }
            }

            return details;

        } catch (Exception e) {
            log.debug("Failed to fetch product details from {}: {}", url, e.getMessage());
            return current;
        }
    }

    /**
     * Извлечение валюты
     */
    private String extractCurrency(Element element) {
        String text = element.text().toLowerCase();

        if (text.contains("₽") || text.contains("руб") || text.contains("rub")) {
            return "RUB";
        } else if (text.contains("$") || text.contains("usd") || text.contains("долл")) {
            return "USD";
        } else if (text.contains("€") || text.contains("eur") || text.contains("евро")) {
            return "EUR";
        }

        return null; // Будет использовано значение по умолчанию
    }

    /**
     * Извлечение единицы измерения
     */
    private String extractUnit(Element element) {
        String text = element.text().toLowerCase();

        if (text.contains("/т") || text.contains("за тонну") || text.contains("тонна")) {
            return "т";
        } else if (text.contains("/м") || text.contains("за метр") || text.contains("метр")) {
            return "м";
        } else if (text.contains("/кг") || text.contains("за кг") || text.contains("килограмм")) {
            return "кг";
        } else if (text.contains("/шт") || text.contains("за штуку") || text.contains("штука")) {
            return "шт";
        }

        return null; // Будет использовано значение по умолчанию
    }

    /**
     * Вспомогательный класс для хранения размеров
     */
    private static class ProductDimensions {
        String size = "";
        String thickness = "";
        String steelGrade = "";
        String length = "";
    }
}
