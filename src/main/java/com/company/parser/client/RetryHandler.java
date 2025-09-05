package com.company.parser.client;

import com.company.parser.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Обработчик повторных попыток
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryHandler {

    private final AppProperties appProperties;

    /**
     * Выполнение операции с повторными попытками
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation) throws IOException {
        int maxAttempts = appProperties.http().maxRetries();
        int baseDelayMs = appProperties.http().retryDelayMs();

        IOException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute();

            } catch (IOException e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    long delay = calculateDelay(attempt, baseDelayMs);
                    log.debug("Attempt {}/{} failed, retrying after {}ms: {}",
                            attempt, maxAttempts, delay, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                } else {
                    log.error("All {} attempts failed", maxAttempts);
                }
            }
        }

        throw lastException;
    }

    /**
     * Расчет задержки с экспоненциальным увеличением и jitter
     */
    private long calculateDelay(int attempt, int baseDelayMs) {
        // Экспоненциальная задержка: base * 2^(attempt-1)
        long exponentialDelay = baseDelayMs * (1L << (attempt - 1));

        // Максимальная задержка 30 секунд
        long maxDelay = 30_000;
        long delay = Math.min(exponentialDelay, maxDelay);

        // Добавление jitter (±25%)
        long jitter = ThreadLocalRandom.current().nextLong(
                (long) (delay * 0.75),
                (long) (delay * 1.25)
        );

        return jitter;
    }

    /**
     * Функциональный интерфейс для операции с повторами
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws IOException;
    }
}
