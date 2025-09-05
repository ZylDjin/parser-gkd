package com.company.parser.mapper;

import com.company.parser.model.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Маппер для уведомлений
 */
@Component
public class NotificationMapper {

    /**
     * Создание NotificationDTO для изменений цен
     */
    public NotificationDTO toPriceChangeNotification(List<PriceChangeDTO> changes,
                                                     StatisticsDTO statistics) {

        String subject = generateSubject(changes);
        String message = generateMessage(changes, statistics);

        return NotificationDTO.builder()
                .type(NotificationDTO.NotificationType.PRICE_CHANGE)
                .subject(subject)
                .message(message)
                .priceChanges(changes)
                .statistics(statistics)
                .timestamp(Instant.now())
                .priority(calculatePriority(changes))
                .build();
    }

    /**
     * Создание уведомления об ошибке
     */
    public NotificationDTO toErrorNotification(String error, String details) {
        return NotificationDTO.builder()
                .type(NotificationDTO.NotificationType.PARSING_ERROR)
                .subject("❌ Ошибка парсинга")
                .message(String.format("Произошла ошибка: %s\n\nДетали:\n%s", error, details))
                .timestamp(Instant.now())
                .priority(NotificationDTO.NotificationPriority.HIGH)
                .build();
    }

    /**
     * Создание уведомления о завершении парсинга
     */
    public NotificationDTO toCompletionNotification(ParsingResultDTO result,
                                                    StatisticsDTO statistics) {

        String subject = result.isSuccess() ?
                "✅ Парсинг успешно завершен" :
                "⚠️ Парсинг завершен с ошибками";

        String message = String.format(
                "Парсинг завершен\n" +
                        "Время выполнения: %s\n" +
                        "Обработано записей: %d\n" +
                        "Успешных: %d\n" +
                        "С ошибками: %d",
                result.getDuration(),
                result.getRecordsCount(),
                result.getSuccessfulRecords(),
                result.getFailedRecords()
        );

        return NotificationDTO.builder()
                .type(NotificationDTO.NotificationType.PARSING_COMPLETE)
                .subject(subject)
                .message(message)
                .statistics(statistics)
                .timestamp(Instant.now())
                .priority(NotificationDTO.NotificationPriority.NORMAL)
                .build();
    }

    private String generateSubject(List<PriceChangeDTO> changes) {
        long criticalCount = changes.stream()
                .filter(c -> c.getChangeType() != null && c.getChangeType().contains("CRITICAL"))
                .count();

        if (criticalCount > 0) {
            return String.format("⚠️ Критические изменения цен (%d)", criticalCount);
        }

        return String.format("📊 Изменения цен (%d)", changes.size());
    }

    private String generateMessage(List<PriceChangeDTO> changes, StatisticsDTO statistics) {
        StringBuilder message = new StringBuilder();

        message.append("Обнаружены изменения цен:\n\n");

        if (statistics != null) {
            message.append(String.format("📈 Повышений: %d\n", statistics.getPriceIncreases()));
            message.append(String.format("📉 Снижений: %d\n", statistics.getPriceDecreases()));
            message.append(String.format("🆕 Новых товаров: %d\n", statistics.getNewProducts()));
            message.append(String.format("❌ Удаленных: %d\n\n", statistics.getRemovedProducts()));
        }

        message.append("Детали изменений доступны в отчете.");

        return message.toString();
    }

    private NotificationDTO.NotificationPriority calculatePriority(List<PriceChangeDTO> changes) {
        boolean hasCritical = changes.stream()
                .anyMatch(c -> c.getChangeType() != null && c.getChangeType().contains("CRITICAL"));

        if (hasCritical) {
            return NotificationDTO.NotificationPriority.HIGH;
        }

        if (changes.size() > 50) {
            return NotificationDTO.NotificationPriority.HIGH;
        }

        if (changes.size() > 10) {
            return NotificationDTO.NotificationPriority.NORMAL;
        }

        return NotificationDTO.NotificationPriority.LOW;
    }
}