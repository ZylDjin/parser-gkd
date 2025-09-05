package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO для уведомления
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    /**
     * Тип уведомления
     */
    private NotificationType type;

    /**
     * Заголовок
     */
    private String subject;

    /**
     * Текст сообщения
     */
    private String message;

    /**
     * HTML версия сообщения
     */
    private String htmlMessage;

    /**
     * Список изменений цен
     */
    private List<PriceChangeDTO> priceChanges;

    /**
     * Статистика
     */
    private StatisticsDTO statistics;

    /**
     * Получатели
     */
    private List<String> recipients;

    /**
     * Время создания
     */
    private Instant timestamp;

    /**
     * Приоритет
     */
    private NotificationPriority priority;

    /**
     * Типы уведомлений
     */
    public enum NotificationType {
        PRICE_CHANGE("Изменение цен"),
        NEW_PRODUCTS("Новые товары"),
        REMOVED_PRODUCTS("Удаленные товары"),
        PARSING_COMPLETE("Парсинг завершен"),
        PARSING_ERROR("Ошибка парсинга"),
        DAILY_REPORT("Ежедневный отчет"),
        WEEKLY_REPORT("Еженедельный отчет");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Приоритет уведомления
     */
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
}
