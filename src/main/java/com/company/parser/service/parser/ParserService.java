package com.company.parser.service.parser;

import com.company.parser.exception.ParserException;
import com.company.parser.mapper.PriceMapper;
import com.company.parser.mapper.ReportMapper;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.dto.*;
import com.company.parser.repository.PriceRepository;
import com.company.parser.service.analysis.PriceAnalysisService;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Основной сервис парсинга
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParserService {

    private final ParserOrchestrator orchestrator;
    private final ScraperService scraperService;
    private final PersistenceService persistenceService;
    private final NotificationService notificationService;
    private final ExportService exportService;
    private final StatisticsService statisticsService;
    private final PriceAnalysisService analysisService;
    private final PriceRepository priceRepository;
    private final PriceMapper priceMapper;
    private final ReportMapper reportMapper;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Запуск полного цикла парсинга
     */
    public ParsingResultDTO executeParsing() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Parsing already in progress");
            return ParsingResultDTO.builder()
                    .success(false)
                    .errorMessage("Parsing already in progress")
                    .build();
        }

        Instant startTime = Instant.now();

        try {
            log.info("Starting parsing process");

            // Делегируем оркестратору
            return orchestrator.executeParsing();

        } catch (Exception e) {
            log.error("Parsing failed", e);

            Duration duration = Duration.between(startTime, Instant.now());

            return ParsingResultDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .build();

        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Асинхронный запуск парсинга
     */
    public CompletableFuture<ParsingResultDTO> executeParsingAsync() {
        return CompletableFuture.supplyAsync(this::executeParsing);
    }

    /**
     * Парсинг конкретного сайта
     */
    public ParsingResultDTO parseSite(String siteId) {
        Instant startTime = Instant.now();

        try {
            log.info("Starting parsing for site: {}", siteId);

            List<PriceRecord> records = scraperService.scrapeSite(siteId);

            if (!records.isEmpty()) {
                persistenceService.saveData(records);
            }

            Duration duration = Duration.between(startTime, Instant.now());
            StatisticsDTO statistics = statisticsService.calculateStatistics(records, null);

            return ParsingResultDTO.builder()
                    .success(true)
                    .recordsCount(records.size())
                    .successfulRecords(records.size())
                    .duration(duration)
                    .statistics(statistics)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse site {}", siteId, e);

            return ParsingResultDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .build();
        }
    }

    /**
     * Получение текущего статуса парсинга
     */
    public boolean isRunning() {
        return isRunning.get() || orchestrator.isRunning();
    }

    /**
     * Остановка парсинга (если поддерживается)
     */
    public void stopParsing() {
        log.info("Stop parsing requested");
        // Реализация остановки если необходимо
        isRunning.set(false);
    }

    /**
     * Получение последних результатов парсинга
     */
    public List<PriceDTO> getLatestResults() {
        List<PriceRecord> records = priceRepository.loadLatest();
        return priceMapper.toDtoList(records);
    }
}
