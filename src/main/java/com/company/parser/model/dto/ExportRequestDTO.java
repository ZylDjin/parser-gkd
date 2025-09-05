package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO для запроса экспорта данных
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequestDTO {

    /**
     * Формат экспорта
     */
    @NotNull(message = "Export format is required")
    private ExportFormat format;

    /**
     * Список сайтов для экспорта (null = все)
     */
    private List<String> siteIds;

    /**
     * Включить лист сравнения
     */
    @Builder.Default
    private boolean includeComparison = true;

    /**
     * Включить историю изменений
     */
    @Builder.Default
    private boolean includeHistory = false;

    /**
     * Включить графики
     */
    @Builder.Default
    private boolean includeCharts = false;

    /**
     * Путь к шаблону (для Excel)
     */
    private String templatePath;

    /**
     * Email для отправки
     */
    private String emailTo;

    /**
     * Форматы экспорта
     */
    public enum ExportFormat {
        EXCEL("xlsx"),
        CSV("csv"),
        JSON("json"),
        XML("xml"),
        PDF("pdf");

        private final String extension;

        ExportFormat(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }
}
