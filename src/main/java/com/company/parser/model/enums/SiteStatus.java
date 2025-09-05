package com.company.parser.model.enums;

/**
 * Статус сайта
 */
public enum SiteStatus {
    ACTIVE("Активен", "Сайт доступен для парсинга"),
    INACTIVE("Неактивен", "Парсинг временно отключен"),
    ERROR("Ошибка", "Сайт недоступен или изменилась структура"),
    MAINTENANCE("Обслуживание", "Сайт на техническом обслуживании"),
    BLOCKED("Заблокирован", "Доступ заблокирован сайтом"),
    DEPRECATED("Устарел", "Сайт больше не поддерживается");

    private final String displayName;
    private final String description;

    SiteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailableForParsing() {
        return this == ACTIVE;
    }
}
