package com.company.parser.exception;

/**
 * Исключение когда данные не найдены
 */
public class DataNotFoundException extends ParserException {

    private String entityType;
    private Object entityId;

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException(String entityType, Object entityId) {
        super(String.format("%s not found with id: %s", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }
}
