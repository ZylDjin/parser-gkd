package com.company.parser.parser.impl;

import com.company.parser.client.HttpClient;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Специализированный парсер для сайта AG Market
 */
@Slf4j
@Component("agMarketParser")
public class AgMarketParser extends CustomSiteParser {

    private static final Pattern ARTICLE_PATTERN = Pattern.compile("Артикул:\\s*(\\S+)");

    @Autowired
    public AgMarketParser(HttpClient httpClient, TextUtils textUtils, SizeNormalizer sizeNormalizer) {
        super(httpClient, textUtils, sizeNormalizer);
    }

    @Override
    protected String getSupportedSiteId() {
        return "agmarket";
    }

    @Override
    protected List<PriceRecord> parsePage(Document document, Site site,
                                          ParsingContext context, String pageUrl) {
        List<PriceRecord> records = new ArrayList<>();

        // AG Market использует другую структуру
        Elements productCards = document.select(".products-list .product-card");

        for (Element card : productCards) {
            try {
                PriceRecord record = parseAgMarketProduct(card, pageUrl);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.debug("Failed to parse AG Market product: {}", e.getMessage());
            }
        }

        context.addRecords(records.size());
        return records;
    }

    @Override
    protected String getNextPageUrl(Document document, Site site) {
        // AG Market использует AJAX пагинацию
        Element pagination = document.selectFirst(".pagination");
        if (pagination != null) {
            Element currentPage = pagination.selectFirst(".pagination__item.active");
            if (currentPage != null) {
                Element nextPage = currentPage.nextElementSibling();
                if (nextPage != null && nextPage.hasClass("pagination__item")) {
                    Element link = nextPage.selectFirst("a");
                    if (link != null) {
                        return link.absUrl("href");
                    }
                }
            }
        }

        return null;
    }

    /**
     * Парсинг товара AG Market
     */
    private PriceRecord parseAgMarketProduct(Element card, String pageUrl) {
        // Название товара
        String name = extractText(card, ".product-card__title");
        if (name.isBlank()) {
            return null;
        }

        // Цена
        BigDecimal price = extractAgMarketPrice(card);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // URL
        Element linkElement = card.selectFirst(".product-card__link");
        String productUrl = linkElement != null ? linkElement.absUrl("href") : pageUrl;

        // Артикул
        String article = extractArticle(card);

        // Таблица характеристик
        AgMarketSpecs specs = extractAgMarketSpecs(card);

        return PriceRecord.builder()
                .siteId("agmarket")
                .siteName("AG Market")
                .productName(name)
                .size(specs.size)
                .thickness(specs.thickness)
                .steelGrade(specs.steelGrade)
                .length(specs.length)
                .price(price)
                .url(productUrl)
                .currency("RUB")
                .unit(specs.unit)
                .additionalProperties(java.util.Map.of("article", article))
                .build();
    }

    /**
     * Извлечение цены AG Market
     */
    private BigDecimal extractAgMarketPrice(Element card) {
        // Основная цена
        String mainPrice = extractText(card, ".product-card__price-current");
        if (!mainPrice.isBlank()) {
            return textUtils.extractPrice(mainPrice).orElse(null);
        }

        // Альтернативный селектор
        String altPrice = extractText(card, ".product-card__price span");
        return textUtils.extractPrice(altPrice).orElse(null);
    }

    /**
     * Извлечение артикула
     */
    private String extractArticle(Element card) {
        String articleBlock = extractText(card, ".product-card__article");

        Matcher matcher = ARTICLE_PATTERN.matcher(articleBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Извлечение характеристик AG Market
     */
    private AgMarketSpecs extractAgMarketSpecs(Element card) {
        AgMarketSpecs specs = new AgMarketSpecs();

        // Таблица характеристик
        Elements specRows = card.select(".product-card__specs tr");
        for (Element row : specRows) {
            String label = extractText(row, "td:first-child").toLowerCase();
            String value = extractText(row, "td:last-child");

            if (label.contains("размер") || label.contains("сечение")) {
                specs.size = textUtils.extractSize(value).orElse(value);
            } else if (label.contains("толщина") || label.contains("стенка")) {
                specs.thickness = value;
            } else if (label.contains("марка") || label.contains("сталь")) {
                specs.steelGrade = value;
            } else if (label.contains("длина")) {
                specs.length = value;
            } else if (label.contains("ед") || label.contains("единица")) {
                specs.unit = parseUnit(value);
            }
        }

        // Если не нашли в таблице, ищем в названии
        String name = extractText(card, ".product-card__title");
        if (specs.size.isBlank()) {
            specs.size = textUtils.extractSize(name).orElse("");
        }
        if (specs.thickness.isBlank()) {
            specs.thickness = textUtils.extractThickness(name).orElse("");
        }

        return specs;
    }

    /**
     * Парсинг единицы измерения
     */
    private String parseUnit(String value) {
        if (value == null) return "шт";

        value = value.toLowerCase();
        if (value.contains("тонн") || value.contains("тн")) return "т";
        if (value.contains("метр") || value.contains("м.")) return "м";
        if (value.contains("кг") || value.contains("килограмм")) return "кг";

        return "шт";
    }

    /**
     * Класс для хранения характеристик AG Market
     */
    private static class AgMarketSpecs {
        String size = "";
        String thickness = "";
        String steelGrade = "";
        String length = "";
        String unit = "шт";
    }
}
