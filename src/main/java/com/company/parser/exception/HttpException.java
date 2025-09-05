package com.company.parser.exception;

/**
 * Исключение при HTTP ошибках
 */
public class HttpException extends ParserException {

    private int statusCode;
    private String url;
    private String method;

    public HttpException(String message) {
        super(message);
    }

    public HttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpException(String url, int statusCode, String message) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
    }

    public HttpException(String method, String url, int statusCode, String message) {
        super(String.format("HTTP %s %s failed with status %d: %s",
                method, url, statusCode, message));
        this.method = method;
        this.url = url;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }
}
