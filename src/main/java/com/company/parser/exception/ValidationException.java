package com.company.parser.exception;

import java.util.Map;

/**
 * Исключение при ошибках валидации
 */
public class ValidationException extends ParserException {

    private Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.errors = Map.of(field, message);
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed: " + errors);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}

