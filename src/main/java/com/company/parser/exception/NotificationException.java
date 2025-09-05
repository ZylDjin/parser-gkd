package com.company.parser.exception;

/**
 * Исключение при ошибках отправки уведомлений
 */
public class NotificationException extends ParserException {

    private String recipient;
    private String notificationType;

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationException(String recipient, String notificationType, String message) {
        super(String.format("Failed to send %s notification to %s: %s",
                notificationType, recipient, message));
        this.recipient = recipient;
        this.notificationType = notificationType;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getNotificationType() {
        return notificationType;
    }
}
