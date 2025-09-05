package com.company.parser.model.enums;

/**
 * Форматы экспорта данных
 */
public enum ExportFormat {
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel"),
    CSV("csv", "text/csv", "CSV (Comma-Separated Values)"),
    JSON("json", "application/json", "JSON"),
    XML("xml", "application/xml", "XML"),
    PDF("pdf", "application/pdf", "PDF Document"),
    HTML("html", "text/html", "HTML Page");

    private final String extension;
    private final String mimeType;
    private final String description;

    ExportFormat(String extension, String mimeType, String description) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.description = description;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Получение формата по расширению файла
     */
    public static ExportFormat fromExtension(String extension) {
        if (extension == null) return EXCEL;

        String ext = extension.toLowerCase().replace(".", "");

        for (ExportFormat format : values()) {
            if (format.extension.equals(ext)) {
                return format;
            }
        }

        return EXCEL; // По умолчанию Excel
    }
}
