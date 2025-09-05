package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для сравнения цен между сайтами
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonDTO {

    /**
     * Размер продукта
     */
    private String size;

    /**
     * Толщина
     */
    private String thickness;

    /**
     * Марка стали
     */
    private String steelGrade;

    /**
     * Базовая цена (для сравнения)
     */
    private BigDecimal basePrice;

    /**
     * ID базового сайта
     */
    private String baseSiteId;

    /**
     * Название базового сайта
     */
    private String baseSiteName;

    /**
     * Цены на других сайтах
     */
    private List<SitePriceDTO> sitePrices;

    /**
     * Минимальная цена среди всех сайтов
     */
    private BigDecimal minPrice;

    /**
     * Максимальная цена среди всех сайтов
     */
    private BigDecimal maxPrice;

    /**
     * Разница между мин и макс ценой
     */
    private BigDecimal priceRange;

    /**
     * Процент разницы
     */
    private BigDecimal priceRangePercent;
}
