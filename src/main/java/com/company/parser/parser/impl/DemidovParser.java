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

/**
 * Специализированный парсер для сайта Demidov Steel
 */
@Slf4j
@Component("demidovParser")
public class DemidovParser extends CustomSiteParser {

    @Autowired
    public DemidovParser(HttpClient httpClient, TextUtils textUtils, SizeNormalizer sizeNormalizer) {
        super(httpClient, textUtils, sizeNormalizer);
    }

    @Override
    protected String getSupportedSiteId() {
        return "demidov";
    }

    @Override
    protected List<PriceRecord> parsePage(Document document, Site site,
                                          ParsingContext context, String pageUrl) {
        List<PriceRecord> records = new ArrayList<>();

        // Специфичные селекторы для Demidov Steel
        Elements productCards = document.select(".catalog__list .catalog-item");

        for (Element card : productCards) {
            try {
                PriceRecord record = parseDemidovProduct(card, pageUrl);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.debug("Failed to parse Demidov product: {}", e.getMessage());
            }
        }

        context.addRecords(records.size());
        return records;
    }

    @Override
    protected String getNextPageUrl(Document document, Site site) {
        // Специфичная логика пагинации для Demidov
        Element nextButton = document.selectFirst("a[rel=next]");
        if (nextButton == null) {
            nextButton = document.selectFirst(".pagination__next:not(.disabled)");
        }

        return nextButton != null ? nextButton.absUrl("href") : null;
    }

    /**
     * Парсинг товара Demidov
     */
    private PriceRecord parseDemidovProduct(Element card, String pageUrl) {
        // Название
        String name = extractText(card, ".catalog-item__title");
        if (name.isBlank()) {
            return null;
        }

        // Цена
        String priceText = extractText(card, ".catalog-item__price .price__current");
        BigDecimal price = textUtils.extractPrice(priceText).orElse(null);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // URL
        Element linkElement = card.selectFirst("a.catalog-item__link");
        String productUrl = linkElement != null ? linkElement.absUrl("href") : pageUrl;

        // Характеристики из блока свойств
        String characteristics = extractText(card, ".catalog-item__props");

        // Извлечение размеров
        String size = textUtils.extractSize(name + " " + characteristics).orElse("");
        String thickness = textUtils.extractThickness(characteristics).orElse("");
        String steelGrade = extractDemidovSteelGrade(characteristics);
        String length = extractDemidovLength(characteristics);

        return PriceRecord.builder()
                .siteId("demidov")
                .siteName("Demidov Steel")
                .productName(name)
                .size(size)
                .thickness(thickness)
                .steelGrade(steelGrade)
                .length(length)
                .price(price)
                .url(productUrl)
                .currency("RUB")
                .unit(extractDemidovUnit(card))
                .build();
    }

    /**
     * Извлечение марки стали по правилам Demidov
     */
    private String extractDemidovSteelGrade(String text) {
        if (text == null) return "";

        // Специфичные паттерны для Demidov
        if (text.contains("Ст3сп")) return "Ст3сп";
        if (text.contains("Ст3пс")) return "Ст3пс";
        if (text.contains("09Г2С")) return "09Г2С";

        return textUtils.extractSteelGrade(text).orElse("");
    }

    /**
     * Извлечение длины по правилам Demidov
     */
    private String extractDemidovLength(String text) {
        if (text == null) return "";

        // Ищем паттерны типа "L=6м" или "длина 12 м"
        if (text.contains("L=")) {
            String after = text.substring(text.indexOf("L=") + 2);
            return textUtils.extractLength(after).orElse("");
        }

        return textUtils.extractLength(text).orElse("");
    }

    /**
     * Извлечение единицы измерения
     */
    private String extractDemidovUnit(Element card) {
        String priceBlock = extractText(card, ".catalog-item__price");

        if (priceBlock.contains("/тн")) return "т";
        if (priceBlock.contains("/м")) return "м";
        if (priceBlock.contains("/шт")) return "шт";

        return "шт";
    }
}
