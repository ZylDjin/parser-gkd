package com.company.parser.parser.base;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;

import java.util.List;

/**
 * Интерфейс для всех парсеров сайтов
 */
public interface SiteParser {

    /**
     * Парсинг сайта
     * @param site конфигурация сайта
     * @return список записей о ценах
     */
    List<PriceRecord> parse(Site site);

    /**
     * Получение типа парсера
     */
    String getType();

    /**
     * Поддерживает ли парсер данный сайт
     */
    boolean supports(Site site);
}