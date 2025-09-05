package com.company.parser.parser.selector;

import org.jsoup.nodes.Element;

import java.util.Optional;

/**
 * Стратегия извлечения данных по селектору
 */
public interface SelectorStrategy {

    /**
     * Извлечение текста по селектору
     */
    Optional<String> extractText(Element element, String selector);

    /**
     * Извлечение атрибута по селектору
     */
    Optional<String> extractAttribute(Element element, String selector, String attribute);

    /**
     * Проверка существования элемента
     */
    boolean exists(Element element, String selector);
}
