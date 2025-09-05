package com.company.parser.model.enums;

/**
 * Единицы измерения
 */
public enum Unit {
    PIECE("шт", "штука", 1.0),
    METER("м", "метр", 1.0),
    TON("т", "тонна", 1000.0),
    KILOGRAM("кг", "килограмм", 1.0),
    SQUARE_METER("м²", "квадратный метр", 1.0),
    LINEAR_METER("пог.м", "погонный метр", 1.0),
    PACKAGE("уп", "упаковка", 1.0),
    SET("компл", "комплект", 1.0);

    private final String shortName;
    private final String fullName;
    private final double baseMultiplier;

    Unit(String shortName, String fullName, double baseMultiplier) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.baseMultiplier = baseMultiplier;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public double getBaseMultiplier() {
        return baseMultiplier;
    }

    /**
     * Получение единицы измерения по короткому имени
     */
    public static Unit fromShortName(String shortName) {
        if (shortName == null) return PIECE;

        String normalized = shortName.toLowerCase().trim();

        for (Unit unit : values()) {
            if (unit.shortName.equalsIgnoreCase(normalized)) {
                return unit;
            }
        }

        // Дополнительные варианты написания
        return switch (normalized) {
            case "штук", "штука", "pcs", "pc" -> PIECE;
            case "метр", "метров", "м.", "m" -> METER;
            case "тонн", "тонна", "t" -> TON;
            case "килограмм", "килограммов", "kg" -> KILOGRAM;
            case "м2", "кв.м", "кв м" -> SQUARE_METER;
            case "погонный метр", "п.м", "п/м" -> LINEAR_METER;
            case "упаковка", "пачка", "pack" -> PACKAGE;
            case "комплект", "набор", "set" -> SET;
            default -> PIECE;
        };
    }
}
