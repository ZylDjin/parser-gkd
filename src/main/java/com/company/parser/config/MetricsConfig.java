package com.company.parser.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;

/**
 * Конфигурация метрик и мониторинга
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsConfig {

    private final AppProperties appProperties;

    /**
     * Аспект для аннотации @Timed
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Аспект для аннотации @Counted
     */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    /**
     * Настройка MeterRegistry
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                    .commonTags(
                            "application", "steel-parser",
                            "environment", getEnvironment(),
                            "version", getVersion()
                    );

            // Добавление фильтров
            registry.config()
                    .meterFilter(MeterFilter.deny(id -> {
                        String uri = id.getTag("uri");
                        return uri != null && uri.startsWith("/actuator");
                    }))
                    .meterFilter(MeterFilter.denyNameStartsWith("jvm.buffer"));

            log.info("Metrics registry configured with common tags");
        };
    }

    /**
     * Prometheus Registry для экспорта метрик
     */
    @Bean
    @ConditionalOnProperty(name = "management.metrics.export.prometheus.enabled", havingValue = "true")
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Регистрация JVM метрик
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);

        log.info("Prometheus metrics registry initialized");
        return registry;
    }

    /**
     * Кастомные метрики для парсера
     */
    @Bean
    public ParserMetrics parserMetrics(MeterRegistry registry) {
        return new ParserMetrics(registry);
    }

    /**
     * Класс для кастомных метрик парсера
     */
    public static class ParserMetrics {
        private final MeterRegistry registry;

        // Счетчики
        private final Counter totalExecutions;
        private final Counter successfulExecutions;
        private final Counter failedExecutions;
        private final Counter recordsParsed;
        private final Counter httpRequests;
        private final Counter httpErrors;
        private final Counter cacheHits;
        private final Counter cacheMisses;

        // Таймеры
        private final Timer executionTimer;
        private final Timer httpRequestTimer;
        private final Timer databaseOperationTimer;

        // Gauges
        private volatile double lastExecutionDuration = 0;
        private volatile int lastRecordCount = 0;
        private volatile long lastExecutionTime = 0;

        public ParserMetrics(MeterRegistry registry) {
            this.registry = registry;

            // Инициализация счетчиков
            this.totalExecutions = Counter.builder("parser.executions.total")
                    .description("Total number of parser executions")
                    .register(registry);

            this.successfulExecutions = Counter.builder("parser.executions.success")
                    .description("Number of successful parser executions")
                    .register(registry);

            this.failedExecutions = Counter.builder("parser.executions.failed")
                    .description("Number of failed parser executions")
                    .register(registry);

            this.recordsParsed = Counter.builder("parser.records.parsed")
                    .description("Total number of records parsed")
                    .register(registry);

            this.httpRequests = Counter.builder("parser.http.requests")
                    .description("Total number of HTTP requests")
                    .register(registry);

            this.httpErrors = Counter.builder("parser.http.errors")
                    .description("Total number of HTTP errors")
                    .register(registry);

            this.cacheHits = Counter.builder("parser.cache.hits")
                    .description("Number of cache hits")
                    .register(registry);

            this.cacheMisses = Counter.builder("parser.cache.misses")
                    .description("Number of cache misses")
                    .register(registry);

            // Инициализация таймеров
            this.executionTimer = Timer.builder("parser.execution.duration")
                    .description("Parser execution duration")
                    .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .register(registry);

            this.httpRequestTimer = Timer.builder("parser.http.duration")
                    .description("HTTP request duration")
                    .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                    .register(registry);

            this.databaseOperationTimer = Timer.builder("parser.db.operation.duration")
                    .description("Database operation duration")
                    .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                    .register(registry);

            // Инициализация Gauges
            Gauge.builder("parser.last.execution.duration", this, ParserMetrics::getLastExecutionDuration)
                    .description("Duration of the last parser execution in seconds")
                    .register(registry);

            Gauge.builder("parser.last.record.count", this, ParserMetrics::getLastRecordCount)
                    .description("Number of records from the last execution")
                    .register(registry);

            Gauge.builder("parser.last.execution.time", this, ParserMetrics::getLastExecutionTime)
                    .description("Timestamp of the last execution")
                    .register(registry);
        }

        // Методы для записи метрик

        public void recordExecution() {
            totalExecutions.increment();
        }

        public void recordSuccess(Duration duration, int recordCount) {
            successfulExecutions.increment();
            executionTimer.record(duration);
            recordsParsed.increment(recordCount);

            this.lastExecutionDuration = duration.toSeconds();
            this.lastRecordCount = recordCount;
            this.lastExecutionTime = System.currentTimeMillis();
        }

        public void recordFailure() {
            failedExecutions.increment();
            this.lastExecutionTime = System.currentTimeMillis();
        }

        public void recordHttpRequest(Duration duration) {
            httpRequests.increment();
            httpRequestTimer.record(duration);
        }

        public void recordHttpError() {
            httpErrors.increment();
        }

        public void recordCacheHit() {
            cacheHits.increment();
        }

        public void recordCacheMiss() {
            cacheMisses.increment();
        }

        public Timer.Sample startTimer() {
            return Timer.start(registry);
        }

        public void recordDatabaseOperation(Duration duration) {
            databaseOperationTimer.record(duration);
        }

        // Getters для Gauges

        public double getLastExecutionDuration() {
            return lastExecutionDuration;
        }

        public int getLastRecordCount() {
            return lastRecordCount;
        }

        public long getLastExecutionTime() {
            return lastExecutionTime;
        }
    }

    /**
     * Метрики для HTTP клиента
     */
    @Bean
    public HttpClientMetrics httpClientMetrics(MeterRegistry registry) {
        return new HttpClientMetrics(registry);
    }

    /**
     * Класс метрик HTTP клиента
     */
    public static class HttpClientMetrics {
        private final MeterRegistry registry;
        private final Timer requestTimer;
        private final Counter requestCounter;
        private final Counter errorCounter;

        public HttpClientMetrics(MeterRegistry registry) {
            this.registry = registry;

            this.requestTimer = Timer.builder("http.client.request.duration")
                    .description("HTTP client request duration")
                    .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                    .register(registry);

            this.requestCounter = Counter.builder("http.client.requests.total")
                    .description("Total HTTP client requests")
                    .register(registry);

            this.errorCounter = Counter.builder("http.client.errors.total")
                    .description("Total HTTP client errors")
                    .register(registry);
        }

        public Timer.Sample startTimer() {
            return Timer.start(registry);
        }

        public void recordRequest(String method, String host, int statusCode, Duration duration) {
            requestCounter.increment();
            requestTimer.record(duration);

            // Дополнительные метрики с тегами
            Timer.builder("http.client.request")
                    .tag("method", method)
                    .tag("host", host)
                    .tag("status", String.valueOf(statusCode))
                    .tag("status.class", getStatusClass(statusCode))
                    .register(registry)
                    .record(duration);
        }

        public void recordError(String host, String errorType) {
            errorCounter.increment();

            Counter.builder("http.client.error")
                    .tag("host", host)
                    .tag("type", errorType)
                    .register(registry)
                    .increment();
        }

        private String getStatusClass(int statusCode) {
            if (statusCode < 200) return "1xx";
            if (statusCode < 300) return "2xx";
            if (statusCode < 400) return "3xx";
            if (statusCode < 500) return "4xx";
            return "5xx";
        }
    }

    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "default");
    }

    private String getVersion() {
        return getClass().getPackage().getImplementationVersion() != null
                ? getClass().getPackage().getImplementationVersion()
                : "unknown";
    }
}
