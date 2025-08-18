package com.company.parser.core.parsers;

import com.company.parser.core.Category;
import com.company.parser.core.PriceVariant;
import com.company.parser.core.SizeKey;
import com.company.parser.core.Competitor;

import java.util.List;

/**
 * Унифицированный интерфейс для парсеров сайтов-конкурентов.
 * Парсер обязан вернуть список кандидатов-цен (PriceVariant) для указанного размера.
 * Сам выбор финальной цены делает PriceSelectorService.
 */
public interface SiteParser {

    /** Какой конкурент реализован этим парсером. */
    Competitor competitor();

    /**
     * Получить кандидатов цен для категории/размера.
     * Возвращаем именно "кандидаты" (может быть несколько), а фильтрацию и выбор минимума
     * выполняет PriceSelectorService.
     */
    List<PriceVariant> fetch(Category category, SizeKey size) throws Exception;
}