package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Конфигурация сайта для парсинга
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    /**
     * Уникальный идентификатор сайта
     */
    private String id;

    /**
     * Название сайта
     */
    private String name;

    /**
     * Стартовый URL для парсинга
     */
    private String startUrl;

    /**
     * Активен ли сайт для парсинга
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Тип парсера (generic, custom)
     */
    @Builder.Default
    private String parserType = "generic";

    /**
     * Валюта по умолчанию
     */
    @Builder.Default
    private String defaultCurrency = "RUB";

    /**
     * Единица измерения по умолчанию
     */
    @Builder.Default
    private String defaultUnit = "шт";

    /**
     * CSS селекторы для парсинга
     */
    private SiteSelectors selectors;

    /**
     * Дополнительные HTTP заголовки
     */
    private Map<String, String> headers;

    /**
     * Конфигурация парсинга
     */
    private ParsingConfig config;
}
