package com.company.parser.model.enums;

/**
 * Типы парсеров
 */
public enum ParserType {
    GENERIC("Универсальный CSS парсер", "Работает с большинством сайтов"),
    CUSTOM("Кастомный парсер", "Специально разработан для конкретного сайта"),
    API("API парсер", "Использует API сайта"),
    JAVASCRIPT("JavaScript парсер", "Для сайтов с динамическим контентом"),
    XML("XML парсер", "Для XML feeds");

    private final String displayName;
    private final String description;

    ParserType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Получение типа парсера по имени
     */
    public static ParserType fromName(String name) {
        if (name == null) return GENERIC;

        try {
            return ParserType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERIC;
        }
    }
}
