package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Агрегированная информация о продукте с разных сайтов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /**
     * Уникальный идентификатор продукта
     */
    private String id;

    /**
     * Название продукта
     */
    private String name;

    /**
     * Размер
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
     * Длина
     */
    private String length;

    /**
     * Категория продукта
     */
    private String category;

    /**
     * Цены на разных сайтах
     */
    private Map<String, ProductPrice> pricesBySite;

    /**
     * Получение минимальной цены
     */
    public ProductPrice getMinPrice() {
        return pricesBySite.values().stream()
                .filter(p -> p.isAvailable())
                .min((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                .orElse(null);
    }

    /**
     * Получение максимальной цены
     */
    public ProductPrice getMaxPrice() {
        return pricesBySite.values().stream()
                .filter(p -> p.isAvailable())
                .max((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                .orElse(null);
    }
}