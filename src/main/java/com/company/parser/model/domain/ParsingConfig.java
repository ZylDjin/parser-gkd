package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Конфигурация парсинга для сайта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsingConfig {

    /**
     * Максимальное количество страниц для парсинга
     */
    @Builder.Default
    private int maxPages = 100;

    /**
     * Максимальное количество записей
     */
    @Builder.Default
    private int maxRecords = 10000;

    /**
     * Задержка между загрузкой страниц (мс)
     */
    @Builder.Default
    private long delayBetweenPages = 500;

    /**
     * Загружать ли детальные страницы товаров
     */
    @Builder.Default
    private boolean fetchProductDetails = false;

    /**
     * Таймаут для загрузки страницы (секунды)
     */
    @Builder.Default
    private int timeout = 30;

    /**
     * Следовать ли редиректам
     */
    @Builder.Default
    private boolean followRedirects = true;

    /**
     * Игнорировать ли SSL ошибки
     */
    @Builder.Default
    private boolean ignoreSslErrors = false;

    /**
     * Использовать ли JavaScript рендеринг
     */
    @Builder.Default
    private boolean useJavaScriptRendering = false;
}
