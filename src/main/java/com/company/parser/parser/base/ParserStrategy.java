package com.company.parser.parser.base;

import com.company.parser.model.domain.PriceRecord;
import org.jsoup.nodes.Element;

/**
 * Стратегия извлечения данных из элемента
 */
public interface ParserStrategy {

    /**
     * Извлечение данных о товаре из элемента
     */
    PriceRecord parseProduct(Element element);

    /**
     * Проверка применимости стратегии
     */
    boolean isApplicable(Element element);
}
