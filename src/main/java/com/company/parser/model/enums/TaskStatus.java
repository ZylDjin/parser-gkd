package com.company.parser.model.enums;

/**
 * Статус задачи парсинга
 */
public enum TaskStatus {
    PENDING("Ожидает", "Задача в очереди"),
    RUNNING("Выполняется", "Задача выполняется"),
    COMPLETED("Завершена", "Задача успешно завершена"),
    FAILED("Ошибка", "Задача завершилась с ошибкой"),
    CANCELLED("Отменена", "Задача была отменена"),
    TIMEOUT("Таймаут", "Превышено время выполнения"),
    SKIPPED("Пропущена", "Задача была пропущена");

    private final String displayName;
    private final String description;

    TaskStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED ||
                this == CANCELLED || this == TIMEOUT;
    }

    public boolean isSuccess() {
        return this == COMPLETED;
    }
}
