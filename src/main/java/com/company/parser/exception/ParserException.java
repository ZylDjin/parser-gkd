package com.company.parser.exception;

/**
 * Базовое исключение для всех ошибок парсера
 */
public class ParserException extends RuntimeException {

    private String errorCode;
    private Object[] args;

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParserException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ParserException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public ParserException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}

