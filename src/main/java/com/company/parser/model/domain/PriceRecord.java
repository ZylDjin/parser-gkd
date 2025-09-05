package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Основная модель записи о цене товара
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRecord {

    /**
     * Уникальный идентификатор записи
     */
    private String id;

    /**
     * Идентификатор сайта-источника
     */
    private String siteId;

    /**
     * Название сайта
     */
    private String siteName;

    /**
     * Название товара/продукта
     */
    private String productName;

    /**
     * Размер (например, "40x20", "100x100")
     */
    private String size;

    /**
     * Толщина стенки (например, "2.0 мм", "3.5 мм")
     */
    private String thickness;

    /**
     * Марка стали (например, "Ст3", "09Г2С")
     */
    private String steelGrade;

    /**
     * Длина (например, "6м", "12м")
     */
    private String length;

    /**
     * Цена товара
     */
    private BigDecimal price;

    /**
     * Валюта (RUB, USD, EUR)
     */
    private String currency;

    /**
     * Единица измерения (шт, м, т, кг)
     */
    private String unit;

    /**
     * URL страницы товара
     */
    private String url;

    /**
     * Временная метка парсинга
     */
    private Instant timestamp;

    /**
     * Дополнительные свойства
     */
    private Map<String, String> additionalProperties;

    /**
     * Проверка валидности записи
     */
    public boolean isValid() {
        return productName != null && !productName.isBlank()
                && price != null && price.compareTo(BigDecimal.ZERO) > 0
                && siteId != null && !siteId.isBlank();
    }

    /**
     * Создание уникального ключа для дедупликации
     */
    public String getUniqueKey() {
        return String.join("|",
                siteId != null ? siteId : "",
                size != null ? size : "",
                thickness != null ? thickness : "",
                steelGrade != null ? steelGrade : ""
        );
    }
}
