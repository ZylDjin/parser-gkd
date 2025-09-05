package com.company.parser.exception;

import java.time.Duration;

/**
 * Исключение при превышении лимита запросов
 */
public class RateLimitException extends ParserException {

    private int limit;
    private Duration retryAfter;

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(int limit, Duration retryAfter) {
        super(String.format("Rate limit exceeded: %d requests. Retry after %s",
                limit, retryAfter));
        this.limit = limit;
        this.retryAfter = retryAfter;
    }

    public int getLimit() {
        return limit;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
