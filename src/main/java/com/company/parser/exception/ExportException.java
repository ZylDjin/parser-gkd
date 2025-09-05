package com.company.parser.exception;

/**
 * Исключение при ошибках экспорта
 */
public class ExportException extends ParserException {

    private String format;
    private String fileName;

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportException(String format, String fileName, String message) {
        super(message);
        this.format = format;
        this.fileName = fileName;
    }

    public String getFormat() {
        return format;
    }

    public String getFileName() {
        return fileName;
    }
}

