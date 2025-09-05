package com.company.parser.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Нормализатор размеров и толщин
 */
@Component
@RequiredArgsConstructor
public class SizeNormalizer {

    private static final Pattern SIZE_CLEANUP_PATTERN = Pattern.compile("[^0-9xх×*]");

    // Карта синонимов размеров
    private static final Map<String, String> SIZE_SYNONYMS = new HashMap<>() {{
        // Различные варианты написания
        put("20х20", "20x20");
        put("20*20", "20x20");
        put("20×20", "20x20");
        put("25х25", "25x25");
        put("30х30", "30x30");
        put("40х20", "40x20");
        put("40х40", "40x40");
        put("50х25", "50x25");
        put("50х50", "50x50");
        put("60х30", "60x30");
        put("60х40", "60x40");
        put("60х60", "60x60");
        put("80х40", "80x40");
        put("80х60", "80x60");
        put("80х80", "80x80");
        put("100х50", "100x50");
        put("100х100", "100x100");
        put("120х60", "120x60");
        put("120х80", "120x80");
        put("120х120", "120x120");
        put("150х100", "150x100");
        put("150х150", "150x150");
    }};

    // Карта синонимов толщин
    private static final Map<String, String> THICKNESS_SYNONYMS = new HashMap<>() {{
        put("1мм", "1.0 мм");
        put("1 мм", "1.0 мм");
        put("1,0 мм", "1.0 мм");
        put("1.5мм", "1.5 мм");
        put("1,5 мм", "1.5 мм");
        put("2мм", "2.0 мм");
        put("2 мм", "2.0 мм");
        put("2,0 мм", "2.0 мм");
        put("2.5мм", "2.5 мм");
        put("2,5 мм", "2.5 мм");
        put("3мм", "3.0 мм");
        put("3 мм", "3.0 мм");
        put("3,0 мм", "3.0 мм");
        put("3.5мм", "3.5 мм");
        put("3,5 мм", "3.5 мм");
        put("4мм", "4.0 мм");
        put("4 мм", "4.0 мм");
        put("4,0 мм", "4.0 мм");
        put("5мм", "5.0 мм");
        put("5 мм", "5.0 мм");
        put("5,0 мм", "5.0 мм");
        put("6мм", "6.0 мм");
        put("6 мм", "6.0 мм");
        put("6,0 мм", "6.0 мм");
    }};

    /**
     * Нормализация размера
     */
    public String normalizeSize(String size) {
        if (size == null || size.isBlank()) {
            return "";
        }

        // Очистка и замена символов
        String normalized = size.toLowerCase()
                .replace('х', 'x')
                .replace('×', 'x')
                .replace('*', 'x')
                .replaceAll("\\s+", "");

        // Проверка синонимов
        return SIZE_SYNONYMS.getOrDefault(normalized, normalized);
    }

    /**
     * Нормализация толщины
     */
    public String normalizeThickness(String thickness) {
        if (thickness == null || thickness.isBlank()) {
            return "";
        }

        // Очистка пробелов
        String normalized = thickness.toLowerCase().trim();

        // Проверка синонимов
        String result = THICKNESS_SYNONYMS.get(normalized);
        if (result != null) {
            return result;
        }

        // Если не найдено в синонимах, пытаемся стандартизировать
        if (normalized.matches("\\d+([.,]\\d+)?\\s*мм?")) {
            String number = normalized.replaceAll("[^0-9.,]", "")
                    .replace(',', '.');
            try {
                double value = Double.parseDouble(number);
                return String.format("%.1f мм", value);
            } catch (NumberFormatException e) {
                return normalized;
            }
        }

        return normalized;
    }

    /**
     * Создание ключа для группировки по размеру и толщине
     */
    public String createKey(String size, String thickness) {
        String normalizedSize = normalizeSize(size);
        String normalizedThickness = normalizeThickness(thickness);

        if (normalizedSize.isEmpty() && normalizedThickness.isEmpty()) {
            return "";
        }

        return normalizedSize + "|" + normalizedThickness;
    }

    /**
     * Проверка валидности размера
     */
    public boolean isValidSize(String size) {
        if (size == null || size.isBlank()) {
            return false;
        }

        String normalized = normalizeSize(size);
        // Проверка формата: числоXчисло или числоXчислоXчисло
        return normalized.matches("\\d+x\\d+(?:x\\d+)?");
    }
}
