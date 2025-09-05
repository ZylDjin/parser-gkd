package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO со статистикой парсинга
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {

    /**
     * Общее количество записей
     */
    private int totalRecords;

    /**
     * Количество записей по сайтам
     */
    private Map<String, Integer> recordsBySite;

    /**
     * Количество уникальных продуктов
     */
    private int uniqueProducts;

    /**
     * Количество уникальных размеров
     */
    private int uniqueSizes;

    /**
     * Средняя цена
     */
    private BigDecimal averagePrice;

    /**
     * Минимальная цена
     */
    private BigDecimal minPrice;

    /**
     * Максимальная цена
     */
    private BigDecimal maxPrice;

    /**
     * Количество повышений цен
     */
    private int priceIncreases;

    /**
     * Количество снижений цен
     */
    private int priceDecreases;

    /**
     * Количество новых продуктов
     */
    private int newProducts;

    /**
     * Количество удаленных продуктов
     */
    private int removedProducts;

    /**
     * Средний процент изменения цен
     */
    private BigDecimal averageChangePercent;
}
