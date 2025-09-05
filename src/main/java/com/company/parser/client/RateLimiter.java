package com.company.parser.client;

import com.company.parser.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Ограничитель частоты запросов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final AppProperties appProperties;
    private Semaphore semaphore;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        int permitsPerSecond = appProperties.http().rateLimitPerSecond();
        semaphore = new Semaphore(permitsPerSecond);

        // Периодическое восстановление пермитов
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::refillPermits, 1, 1, TimeUnit.SECONDS);

        log.debug("RateLimiter initialized with {} permits/second", permitsPerSecond);
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Получение разрешения на выполнение запроса
     */
    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    /**
     * Попытка получить разрешение без блокировки
     */
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    /**
     * Попытка получить разрешение с таймаутом
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return semaphore.tryAcquire(timeout, unit);
    }

    /**
     * Восстановление пермитов
     */
    private void refillPermits() {
        int maxPermits = appProperties.http().rateLimitPerSecond();
        int currentPermits = semaphore.availablePermits();
        int permitsToAdd = maxPermits - currentPermits;

        if (permitsToAdd > 0) {
            semaphore.release(permitsToAdd);
        }
    }

    /**
     * Получение количества доступных пермитов
     */
    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
