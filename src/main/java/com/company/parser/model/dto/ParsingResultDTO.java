package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * DTO с результатами парсинга
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsingResultDTO {

    /**
     * Успешность выполнения
     */
    private boolean success;

    /**
     * Количество обработанных записей
     */
    private int recordsCount;

    /**
     * Количество успешных записей
     */
    private int successfulRecords;

    /**
     * Количество ошибок
     */
    private int failedRecords;

    /**
     * Время выполнения
     */
    private Duration duration;

    /**
     * Время начала
     */
    private Instant startTime;

    /**
     * Время окончания
     */
    private Instant endTime;

    /**
     * Сообщение об ошибке (если есть)
     */
    private String errorMessage;

    /**
     * Статистика по сайтам
     */
    private List<SiteResultDTO> siteResults;

    /**
     * Общая статистика
     */
    private StatisticsDTO statistics;

    /**
     * Создание успешного результата
     */
    public static ParsingResultDTO success(int recordsCount, Duration duration) {
        return ParsingResultDTO.builder()
                .success(true)
                .recordsCount(recordsCount)
                .successfulRecords(recordsCount)
                .duration(duration)
                .build();
    }

    /**
     * Создание результата с ошибкой
     */
    public static ParsingResultDTO failure(String errorMessage) {
        return ParsingResultDTO.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}