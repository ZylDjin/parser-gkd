package com.company.parser.exception;

/**
 * Исключение при ошибках парсинга HTML
 */
public class ParseException extends ParserException {

    private String selector;
    private String html;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(String selector, String message) {
        super(String.format("Failed to parse with selector '%s': %s", selector, message));
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }

    public String getHtml() {
        return html;
    }
}
