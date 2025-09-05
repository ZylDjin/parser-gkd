package com.company.parser.exception;

/**
 * Исключение при ошибках скрапинга
 */
public class ScrapingException extends ParserException {

    private String siteId;
    private String url;
    private int statusCode;

    public ScrapingException(String message) {
        super(message);
    }

    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScrapingException(String siteId, String message) {
        super(message);
        this.siteId = siteId;
    }

    public ScrapingException(String siteId, String url, String message) {
        super(message);
        this.siteId = siteId;
        this.url = url;
    }

    public ScrapingException(String siteId, String url, int statusCode, String message) {
        super(message);
        this.siteId = siteId;
        this.url = url;
        this.statusCode = statusCode;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return String.format("ScrapingException[site=%s, url=%s, status=%d]: %s",
                siteId, url, statusCode, getMessage());
    }
}