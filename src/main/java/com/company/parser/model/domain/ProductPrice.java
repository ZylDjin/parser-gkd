package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Цена продукта на конкретном сайте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPrice {

    /**
     * Идентификатор сайта
     */
    private String siteId;

    /**
     * Название сайта
     */
    private String siteName;

    /**
     * Цена
     */
    private BigDecimal price;

    /**
     * Валюта
     */
    private String currency;

    /**
     * Единица измерения
     */
    private String unit;

    /**
     * URL товара
     */
    private String url;

    /**
     * Время последнего обновления
     */
    private Instant lastUpdated;

    /**
     * Доступен ли товар
     */
    @Builder.Default
    private boolean available = true;

    /**
     * Старая цена (для отслеживания изменений)
     */
    private BigDecimal previousPrice;

    /**
     * Процент изменения цены
     */
    private BigDecimal priceChangePercent;
}
