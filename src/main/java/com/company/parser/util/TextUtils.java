package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилиты для работы с текстом
 */
@Component
public class TextUtils {

    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9][0-9\\s.,]*)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+\\s*[xXхХ*]\\s*\\d+)(?:\\s*[xXхХ*]\\s*\\d+)?");
    private static final Pattern THICKNESS_PATTERN = Pattern.compile("(\\d+[.,]?\\d*)\\s*(?:мм|mm)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LENGTH_PATTERN = Pattern.compile("(\\d+(?:[,.]\\d+)?\\s*(?:м|метр|m)(?!м))", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEEL_GRADE_PATTERN = Pattern.compile("(Ст3|09Г2С|10ХСНД|15ХСНД|20|35|45|AISI\\s*\\d{3})", Pattern.CASE_INSENSITIVE);

    /**
     * Нормализация текста (удаление лишних пробелов, спецсимволов)
     */
    public String normalize(String text) {
        if (text == null) return "";

        return text
                .replace('\u00A0', ' ')  // неразрывный пробел
                .replace('\u200B', ' ')  // zero-width space
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Извлечение цены из текста
     */
    public Optional<BigDecimal> extractPrice(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        // Удаляем все символы кроме цифр, точек, запятых и пробелов
        String cleaned = text.replaceAll("[^0-9.,\\s]", "");

        Matcher matcher = PRICE_PATTERN.matcher(cleaned);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String priceStr = matcher.group(1)
                .replace(" ", "")
                .replace(",", ".");

        try {
            BigDecimal price = new BigDecimal(priceStr);
            // Проверка на адекватность цены
            if (price.compareTo(BigDecimal.ZERO) <= 0 ||
                    price.compareTo(new BigDecimal("10000000")) > 0) {
                return Optional.empty();
            }
            return Optional.of(price);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Извлечение размера из текста (например, "40x20", "100x100")
     */
    public Optional<String> extractSize(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = SIZE_PATTERN.matcher(text);
        if (matcher.find()) {
            String size = normalize(matcher.group(1))
                    .replaceAll("\\s*", "")
                    .replace('х', 'x')
                    .replace('Х', 'x')
                    .replace('*', 'x');
            return Optional.of(size);
        }

        return Optional.empty();
    }

    /**
     * Извлечение толщины из текста
     */
    public Optional<String> extractThickness(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = THICKNESS_PATTERN.matcher(text);
        if (matcher.find()) {
            String thickness = normalize(matcher.group(1))
                    .replace(',', '.');
            return Optional.of(thickness + " мм");
        }

        return Optional.empty();
    }

    /**
     * Извлечение длины из текста
     */
    public Optional<String> extractLength(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = LENGTH_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }

        return Optional.empty();
    }

    /**
     * Извлечение марки стали
     */
    public Optional<String> extractSteelGrade(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = STEEL_GRADE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).toUpperCase());
        }

        return Optional.empty();
    }

    /**
     * Очистка HTML тегов
     */
    public String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Обрезка текста до максимальной длины
     */
    public String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
