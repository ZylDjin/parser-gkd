package com.company.parser.service.parser;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.dto.ParsingResultDTO;
import com.company.parser.model.dto.SiteResultDTO;
import com.company.parser.model.dto.StatisticsDTO;
import com.company.parser.service.analysis.ComparisonService;
import com.company.parser.service.analysis.StatisticsService;
import com.company.parser.service.export.ExportService;
import com.company.parser.service.notification.NotificationService;
import com.company.parser.service.persistence.PersistenceService;
import com.company.parser.service.scraper.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Оркестратор процесса парсинга - координирует работу всех сервисов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParserOrchestrator {

    private final ScraperService scraperService;
    private final PersistenceService persistenceService;
    private final ExportService exportService;
    private final NotificationService notificationService;
    private final StatisticsService statisticsService;
    private final ComparisonService comparisonService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Запуск полного цикла парсинга
     */
    public ParsingResultDTO executeParsing() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Orchestration already in progress");
            return ParsingResultDTO.builder()
                    .success(false)
                    .errorMessage("Parsing already in progress")
                    .build();
        }

        Instant startTime = Instant.now();
        List<SiteResultDTO> siteResults = new ArrayList<>();

        try {
            log.info("Starting parsing orchestration");

            // 1. Загрузка предыдущих данных для сравнения
            List<PriceRecord> previousData = loadPreviousData();
            log.info("Loaded {} previous records", previousData.size());

            // 2. Скрапинг новых данных со всех сайтов
            List<PriceRecord> currentData = scraperService.scrapeAllSites();
            log.info("Scraped {} new records", currentData.size());

            if (currentData.isEmpty()) {
                throw new RuntimeException("No data was scraped");
            }

            // 3. Анализ и валидация данных
            currentData = validateAndProcess(currentData);

            // 4. Сохранение данных
            persistData(currentData);

            // 5. Генерация сравнений
            comparisonService.generateComparisons(currentData, previousData);

            // 6. Экспорт отчетов
            exportReports(currentData, previousData);

            // 7. Отправка уведомлений
            sendNotifications(currentData, previousData);

            // 8. Расчет статистики
            StatisticsDTO statistics = statisticsService.calculateStatistics(currentData, previousData);

            Duration duration = Duration.between(startTime, Instant.now());

            return ParsingResultDTO.builder()
                    .success(true)
                    .recordsCount(currentData.size())
                    .successfulRecords(currentData.size())
                    .failedRecords(0)
                    .duration(duration)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .siteResults(siteResults)
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("Orchestration failed", e);

            Duration duration = Duration.between(startTime, Instant.now());

            // Отправка уведомления об ошибке
            notificationService.sendErrorNotification(e.getMessage());

            return ParsingResultDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .siteResults(siteResults)
                    .build();

        } finally {
            isRunning.set(false);
            log.info("Parsing orchestration completed");
        }
    }

    /**
     * Асинхронный запуск
     */
    public CompletableFuture<ParsingResultDTO> executeParsingAsync() {
        return CompletableFuture.supplyAsync(this::executeParsing);
    }

    /**
     * Проверка статуса выполнения
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    // === Private методы ===

    private List<PriceRecord> loadPreviousData() {
        try {
            return persistenceService.loadLatestData();
        } catch (Exception e) {
            log.warn("Failed to load previous data: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PriceRecord> validateAndProcess(List<PriceRecord> data) {
        // Валидация и обработка данных
        return data.stream()
                .filter(record -> record.isValid())
                .distinct()
                .toList();
    }

    private void persistData(List<PriceRecord> data) {
        try {
            persistenceService.saveData(data);
            log.info("Data persisted successfully");
        } catch (Exception e) {
            log.error("Failed to persist data", e);
            // Не прерываем процесс, продолжаем с экспортом
        }
    }

    private void exportReports(List<PriceRecord> current, List<PriceRecord> previous) {
        try {
            exportService.exportAll(current, previous);
            log.info("Reports exported successfully");
        } catch (Exception e) {
            log.error("Failed to export reports", e);
        }
    }

    private void sendNotifications(List<PriceRecord> current, List<PriceRecord> previous) {
        try {
            if (!previous.isEmpty()) {
                notificationService.processNotifications(current, previous);
                log.info("Notifications sent successfully");
            }
        } catch (Exception e) {
            log.error("Failed to send notifications", e);
        }
    }
}

