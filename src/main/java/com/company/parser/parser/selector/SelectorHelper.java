package com.company.parser.parser.selector;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Вспомогательный класс для работы с селекторами
 */
@Component
public class SelectorHelper {

    private static final Pattern TEXT_PATTERN = Pattern.compile("text\\(\\)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern CONTAINS_PATTERN = Pattern.compile("contains\\(([^,]+),\\s*['\"]([^'\"]+)['\"]\\)");

    /**
     * Поиск элемента по тексту
     */
    public Optional<Element> findByText(Element parent, String text) {
        if (parent == null || text == null) {
            return Optional.empty();
        }

        Elements elements = parent.getAllElements();

        for (Element element : elements) {
            if (text.equals(element.ownText().trim())) {
                return Optional.of(element);
            }
        }

        return Optional.empty();
    }

    /**
     * Поиск элементов содержащих текст
     */
    public List<Element> findByTextContaining(Element parent, String text) {
        List<Element> result = new ArrayList<>();

        if (parent == null || text == null) {
            return result;
        }

        Elements elements = parent.getAllElements();

        for (Element element : elements) {
            if (element.ownText().contains(text)) {
                result.add(element);
            }
        }

        return result;
    }

    /**
     * Поиск следующего элемента по селектору
     */
    public Optional<Element> findNextSibling(Element element, String selector) {
        if (element == null) {
            return Optional.empty();
        }

        Element sibling = element.nextElementSibling();

        while (sibling != null) {
            if (selector == null || selector.isBlank() || sibling.is(selector)) {
                return Optional.of(sibling);
            }
            sibling = sibling.nextElementSibling();
        }

        return Optional.empty();
    }

    /**
     * Поиск предыдущего элемента по селектору
     */
    public Optional<Element> findPreviousSibling(Element element, String selector) {
        if (element == null) {
            return Optional.empty();
        }

        Element sibling = element.previousElementSibling();

        while (sibling != null) {
            if (selector == null || selector.isBlank() || sibling.is(selector)) {
                return Optional.of(sibling);
            }
            sibling = sibling.previousElementSibling();
        }

        return Optional.empty();
    }

    /**
     * Извлечение чисел из текста
     */
    public List<String> extractNumbers(String text) {
        List<String> numbers = new ArrayList<>();

        if (text == null) {
            return numbers;
        }

        Pattern pattern = Pattern.compile("\\d+([.,]\\d+)?");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        return numbers;
    }

    /**
     * Комбинирование селекторов
     */
    public String combineSelectors(String... selectors) {
        if (selectors == null || selectors.length == 0) {
            return "";
        }

        return String.join(", ", selectors);
    }
}
