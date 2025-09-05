package com.company.parser.model.enums;

/**
 * Типы изменений цен
 */
public enum ChangeType {
    INCREASE("Повышение", "↑", "#ff4444"),
    DECREASE("Снижение", "↓", "#44ff44"),
    CRITICAL_INCREASE("Критическое повышение", "⬆", "#ff0000"),
    CRITICAL_DECREASE("Критическое снижение", "⬇", "#00ff00"),
    NO_CHANGE("Без изменений", "=", "#888888"),
    NEW_PRODUCT("Новый товар", "★", "#4444ff"),
    REMOVED_PRODUCT("Товар удален", "✖", "#ff8800");

    private final String description;
    private final String symbol;
    private final String color;

    ChangeType(String description, String symbol, String color) {
        this.description = description;
        this.symbol = symbol;
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    /**
     * Определение типа изменения по процентам
     */
    public static ChangeType fromPercent(double percent, double criticalThreshold) {
        if (Math.abs(percent) < 0.01) {
            return NO_CHANGE;
        } else if (percent >= criticalThreshold) {
            return CRITICAL_INCREASE;
        } else if (percent <= -criticalThreshold) {
            return CRITICAL_DECREASE;
        } else if (percent > 0) {
            return INCREASE;
        } else {
            return DECREASE;
        }
    }
}
