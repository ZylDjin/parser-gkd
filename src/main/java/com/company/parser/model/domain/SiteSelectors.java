package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CSS селекторы для парсинга сайта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteSelectors {

    /**
     * Селектор списка товаров на странице
     */
    private String listSelector;

    /**
     * Селектор названия товара
     */
    private String nameSelector;

    /**
     * Селектор цены
     */
    private String priceSelector;

    /**
     * Селектор ссылки на товар
     */
    private String urlSelector;

    /**
     * Селектор следующей страницы
     */
    private String nextPageSelector;

    /**
     * Селектор блока с размерами
     */
    private String sizeSelector;

    /**
     * Селектор толщины (опционально)
     */
    private String thicknessSelector;

    /**
     * Селектор марки стали (опционально)
     */
    private String steelGradeSelector;

    /**
     * Селектор длины (опционально)
     */
    private String lengthSelector;

    /**
     * Селектор единицы измерения (опционально)
     */
    private String unitSelector;

    /**
     * Селектор наличия товара (опционально)
     */
    private String availabilitySelector;
}

