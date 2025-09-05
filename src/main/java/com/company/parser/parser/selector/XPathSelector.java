package com.company.parser.parser.selector;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * XPath селектор стратегия (заглушка, JSoup не поддерживает XPath напрямую)
 * Можно расширить с использованием дополнительных библиотек
 */
@Slf4j
@Component
public class XPathSelector implements SelectorStrategy {

    @Override
    public Optional<String> extractText(Element element, String selector) {
        // JSoup не поддерживает XPath напрямую
        // Можно использовать библиотеку us.codecraft.xsoup для XPath поддержки
        log.warn("XPath selectors are not directly supported by JSoup");
        return Optional.empty();
    }

    @Override
    public Optional<String> extractAttribute(Element element, String selector, String attribute) {
        log.warn("XPath selectors are not directly supported by JSoup");
        return Optional.empty();
    }

    @Override
    public boolean exists(Element element, String selector) {
        return false;
    }
}
