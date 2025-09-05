package com.company.parser.parser.selector;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * CSS селектор стратегия
 */
@Slf4j
@Component
public class CssSelector implements SelectorStrategy {

    @Override
    public Optional<String> extractText(Element element, String selector) {
        if (element == null || selector == null || selector.isBlank()) {
            return Optional.empty();
        }

        try {
            // Поддержка множественных селекторов через |
            String[] selectors = selector.split("\\|");

            for (String sel : selectors) {
                Element found = element.selectFirst(sel.trim());
                if (found != null) {
                    String text = found.text();
                    if (!text.isBlank()) {
                        return Optional.of(text.trim());
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract text with selector '{}': {}", selector, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> extractAttribute(Element element, String selector, String attribute) {
        if (element == null || selector == null || attribute == null) {
            return Optional.empty();
        }

        try {
            Element found = element.selectFirst(selector);
            if (found != null) {
                String value = found.attr(attribute);
                if (!value.isBlank()) {
                    return Optional.of(value.trim());
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract attribute '{}' with selector '{}': {}",
                    attribute, selector, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(Element element, String selector) {
        if (element == null || selector == null) {
            return false;
        }

        try {
            return element.selectFirst(selector) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
