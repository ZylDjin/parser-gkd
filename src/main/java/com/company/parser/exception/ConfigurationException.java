package com.company.parser.exception;

/**
 * Исключение при ошибках конфигурации
 */
public class ConfigurationException extends ParserException {

    private String configKey;
    private Object invalidValue;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(String configKey, Object invalidValue, String message) {
        super(message);
        this.configKey = configKey;
        this.invalidValue = invalidValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}
