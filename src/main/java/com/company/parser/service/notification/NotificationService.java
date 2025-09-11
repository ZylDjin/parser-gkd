package com.company.parser.service.notification;

import com.company.parser.model.domain.PriceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * Обработка уведомлений об изменениях
     */
    public void processNotifications(List<PriceRecord> current, List<PriceRecord> previous) {
        log.info("Processing notifications for {} current and {} previous records",
                current.size(), previous.size());
        // TODO: Implement email notifications
    }

    /**
     * Отправка уведомления об ошибке
     */
    public void sendErrorNotification(String error) {
        log.error("Error notification: {}", error);
        // TODO: Implement error notification
    }

    /**
     * Отправка уведомления о завершении парсинга
     */
    public void notifyParsingComplete(com.company.parser.model.dto.ParsingResultDTO result) {
        log.info("Parsing complete notification: {} records", result.getRecordsCount());
        // TODO: Implement completion notification
    }
}
