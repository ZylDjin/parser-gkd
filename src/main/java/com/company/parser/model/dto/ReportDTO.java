package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO для полного отчета
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    /**
     * Заголовок отчета
     */
    private String title;

    /**
     * Дата и время генерации отчета
     */
    private Instant generatedAt;

    /**
     * Общая статистика
     */
    private StatisticsDTO statistics;

    /**
     * Список сравнений цен по продуктам
     */
    private List<ComparisonDTO> comparisons;

    /**
     * Список изменений цен
     */
    private List<PriceChangeDTO> priceChanges;

    /**
     * Период отчета (опционально)
     */
    private String period;

    /**
     * Дополнительные примечания
     */
    private String notes;
}
