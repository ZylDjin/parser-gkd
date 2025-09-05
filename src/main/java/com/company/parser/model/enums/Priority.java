package com.company.parser.model.enums;

/**
 * Приоритеты для задач и уведомлений
 */
public enum Priority {
    LOW(0, "Низкий", "#888888"),
    NORMAL(1, "Обычный", "#4444ff"),
    HIGH(2, "Высокий", "#ff8800"),
    CRITICAL(3, "Критический", "#ff0000");

    private final int level;
    private final String displayName;
    private final String color;

    Priority(int level, String displayName, String color) {
        this.level = level;
        this.displayName = displayName;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    /**
     * Сравнение приоритетов
     */
    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
}
