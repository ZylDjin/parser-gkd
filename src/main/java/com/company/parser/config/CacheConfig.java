package com.company.parser.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Конфигурация кэширования с использованием Caffeine
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.http.cacheEnabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    private final AppProperties appProperties;

    /**
     * Основной менеджер кэша
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(buildCaches());
        return manager;
    }

    /**
     * Обработчик ошибок кэширования
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    /**
     * Создание кэшей с разными настройками
     */
    private List<CaffeineCache> buildCaches() {
        return Arrays.asList(
                buildCache("sitesConfig", 100, 60, TimeUnit.MINUTES),
                buildCache("sizesConfig", 100, 60, TimeUnit.MINUTES),
                buildCache("httpResponses", 1000, appProperties.http().cacheTtlMinutes(), TimeUnit.MINUTES),
                buildCache("parsedData", 500, 10, TimeUnit.MINUTES),
                buildCache("statistics", 50, 5, TimeUnit.MINUTES),
                buildCache("priceHistory", 200, 30, TimeUnit.MINUTES),
                buildCache("exports", 20, 60, TimeUnit.MINUTES)
        );
    }

    /**
     * Создание кэша с указанными параметрами
     */
    private CaffeineCache buildCache(String name, int maxSize, long duration, TimeUnit unit) {
        log.info("Creating cache '{}' with maxSize={}, ttl={} {}", name, maxSize, duration, unit);

        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(duration, unit)
                .recordStats()
                .ticker(ticker())
                .build());
    }

    /**
     * Ticker для тестирования (можно заменить на mock в тестах)
     */
    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Кэш для HTTP ответов
     */
    @Bean
    public Caffeine<Object, Object> httpCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(appProperties.http().cacheTtlMinutes(), TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Кэш для конфигураций
     */
    @Bean
    public Caffeine<Object, Object> configCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Кэш для данных
     */
    @Bean
    public Caffeine<Object, Object> dataCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Кастомный обработчик ошибок кэширования
     */
    private static class CustomCacheErrorHandler extends SimpleCacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key) {
            log.error("Cache get error for cache '{}', key '{}': {}",
                    cache.getName(), key, exception.getMessage());
            // Не пробрасываем исключение, продолжаем работу без кэша
        }

        @Override
        public void handleCachePutError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key,
                                        Object value) {
            log.error("Cache put error for cache '{}', key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception,
                                          org.springframework.cache.Cache cache,
                                          Object key) {
            log.error("Cache evict error for cache '{}', key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception,
                                          org.springframework.cache.Cache cache) {
            log.error("Cache clear error for cache '{}': {}",
                    cache.getName(), exception.getMessage());
        }
    }
}