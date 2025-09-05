package com.company.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/**
 * Основная конфигурация приложения
 * Маппится из application.yml с префиксом "app"
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @Valid Output output,
        @Valid Http http,
        @Valid YamlPaths yaml,
        @Valid Compare compare,
        @Valid Notification notification,
        @Valid Persistence persistence,
        @Valid Scheduler scheduler,
        @Valid Monitoring monitoring,
        @Valid Export export,
        @Valid Security security
) {

    /**
     * Настройки вывода результатов
     */
    public record Output(
            @NotBlank(message = "Output directory is required")
            String dir,

            String templatePath,

            @NotNull
            Boolean archiveEnabled,

            String archiveDir,

            @Min(1) @Max(365)
            Integer archiveRetentionDays
    ) {
        public Output {
            // Установка значений по умолчанию
            archiveEnabled = archiveEnabled != null ? archiveEnabled : true;
            archiveDir = archiveDir != null ? archiveDir : dir + "/archive";
            archiveRetentionDays = archiveRetentionDays != null ? archiveRetentionDays : 30;
        }
    }

    /**
     * Настройки HTTP клиента
     */
    public record Http(
            @Min(5) @Max(300)
            Integer timeoutSec,

            @NotBlank
            String userAgent,

            @Min(0) @Max(10)
            Integer maxRetries,

            @Min(100) @Max(60000)
            Integer retryDelayMs,

            @Min(1) @Max(50)
            Integer maxConcurrentRequests,

            @Min(1) @Max(100)
            Integer rateLimitPerSecond,

            Boolean cacheEnabled,

            @Min(1) @Max(60)
            Integer cacheTtlMinutes,

            Map<String, String> defaultHeaders,

            ProxySettings proxy
    ) {
        public Http {
            // Значения по умолчанию
            timeoutSec = timeoutSec != null ? timeoutSec : 30;
            userAgent = userAgent != null ? userAgent : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
            maxRetries = maxRetries != null ? maxRetries : 3;
            retryDelayMs = retryDelayMs != null ? retryDelayMs : 1000;
            maxConcurrentRequests = maxConcurrentRequests != null ? maxConcurrentRequests : 10;
            rateLimitPerSecond = rateLimitPerSecond != null ? rateLimitPerSecond : 5;
            cacheEnabled = cacheEnabled != null ? cacheEnabled : true;
            cacheTtlMinutes = cacheTtlMinutes != null ? cacheTtlMinutes : 5;
        }
    }

    /**
     * Настройки прокси
     */
    public record ProxySettings(
            Boolean enabled,
            String host,
            Integer port,
            String username,
            String password,
            List<String> nonProxyHosts
    ) {}

    /**
     * Пути к YAML конфигурациям
     */
    public record YamlPaths(
            @NotBlank(message = "Sites config path is required")
            String sites,

            @NotBlank(message = "Sizes config path is required")
            String sizes,

            String customParsers,

            Boolean watchForChanges
    ) {
        public YamlPaths {
            watchForChanges = watchForChanges != null ? watchForChanges : false;
        }
    }

    /**
     * Настройки сравнения цен
     */
    public record Compare(
            @NotBlank
            String baseSiteId,

            @NotNull
            Boolean createSheet,

            @Min(0) @Max(100)
            Double priceChangeThresholdPercent,

            @Min(0) @Max(100)
            Double criticalChangeThresholdPercent,

            Boolean trackHistory,

            ComparisonMode mode
    ) {
        public Compare {
            createSheet = createSheet != null ? createSheet : true;
            priceChangeThresholdPercent = priceChangeThresholdPercent != null ? priceChangeThresholdPercent : 5.0;
            criticalChangeThresholdPercent = criticalChangeThresholdPercent != null ? criticalChangeThresholdPercent : 15.0;
            trackHistory = trackHistory != null ? trackHistory : true;
            mode = mode != null ? mode : ComparisonMode.PERCENTAGE;
        }

        public enum ComparisonMode {
            PERCENTAGE,
            ABSOLUTE,
            BOTH
        }
    }

    /**
     * Настройки уведомлений
     */
    public record Notification(
            @NotNull
            Boolean enabled,

            @Email(message = "Invalid email format")
            List<String> recipients,

            @Min(1) @Max(168)
            Integer minIntervalHours,

            SmtpSettings smtp,

            NotificationTypes types,

            Templates templates
    ) {
        public Notification {
            enabled = enabled != null ? enabled : false;
            minIntervalHours = minIntervalHours != null ? minIntervalHours : 24;
        }
    }

    /**
     * SMTP настройки
     */
    public record SmtpSettings(
            @NotBlank String host,
            @Min(1) @Max(65535) Integer port,
            String username,
            String password,
            Boolean enableTls,
            Boolean enableSsl,
            Integer connectionTimeout,
            Map<String, String> properties
    ) {}

    /**
     * Типы уведомлений
     */
    public record NotificationTypes(
            Boolean priceChange,
            Boolean newProducts,
            Boolean removedProducts,
            Boolean errors,
            Boolean dailyReport,
            Boolean weeklyReport
    ) {}

    /**
     * Шаблоны уведомлений
     */
    public record Templates(
            String priceChangePath,
            String errorPath,
            String reportPath
    ) {}

    /**
     * Настройки сохранения данных
     */
    public record Persistence(
            @NotNull
            Boolean enabled,

            @NotBlank
            String dataDir,

            String archiveDir,

            @Min(1) @Max(365)
            Integer historyDays,

            Boolean compressionEnabled,

            DatabaseSettings database,

            BackupSettings backup
    ) {
        public Persistence {
            enabled = enabled != null ? enabled : true;
            dataDir = dataDir != null ? dataDir : "./data";
            archiveDir = archiveDir != null ? archiveDir : dataDir + "/archive";
            historyDays = historyDays != null ? historyDays : 30;
            compressionEnabled = compressionEnabled != null ? compressionEnabled : true;
        }
    }

    /**
     * Настройки базы данных
     */
    public record DatabaseSettings(
            Boolean enabled,
            String url,
            String username,
            String password,
            String driverClassName,
            Integer connectionPoolSize
    ) {}

    /**
     * Настройки резервного копирования
     */
    public record BackupSettings(
            Boolean enabled,
            String schedule,
            String backupDir,
            Integer retentionDays
    ) {}

    /**
     * Настройки планировщика
     */
    public record Scheduler(
            @NotNull
            Boolean enabled,

            @Pattern(regexp = "^[0-9\\s\\*\\?/,-]+$", message = "Invalid cron expression")
            String cronExpression,

            @NotBlank
            String timezone,

            Boolean enableLocking,

            RetrySettings retry,

            List<ScheduleOverride> overrides
    ) {
        public Scheduler {
            enabled = enabled != null ? enabled : false;
            cronExpression = cronExpression != null ? cronExpression : "0 0 8,14,20 * * *";
            timezone = timezone != null ? timezone : "Europe/Moscow";
            enableLocking = enableLocking != null ? enableLocking : true;
        }
    }

    /**
     * Настройки повторных попыток для планировщика
     */
    public record RetrySettings(
            Boolean enabled,
            Integer maxAttempts,
            Long delayMs
    ) {}

    /**
     * Переопределение расписания для конкретных дней
     */
    public record ScheduleOverride(
            String dayOfWeek,
            String cronExpression,
            Boolean enabled
    ) {}

    /**
     * Настройки мониторинга
     */
    public record Monitoring(
            @NotNull
            Boolean enabled,

            Boolean metricsEnabled,

            Boolean healthCheckEnabled,

            String metricsExportPath,

            @Min(1) @Max(3600)
            Integer metricsExportIntervalSeconds,

            AlertSettings alerts
    ) {
        public Monitoring {
            enabled = enabled != null ? enabled : true;
            metricsEnabled = metricsEnabled != null ? metricsEnabled : true;
            healthCheckEnabled = healthCheckEnabled != null ? healthCheckEnabled : true;
            metricsExportIntervalSeconds = metricsExportIntervalSeconds != null ? metricsExportIntervalSeconds : 60;
        }
    }

    /**
     * Настройки алертов
     */
    public record AlertSettings(
            Boolean enabled,
            Integer errorThreshold,
            Integer slowResponseThresholdMs,
            List<String> alertRecipients
    ) {}

    /**
     * Настройки экспорта
     */
    public record Export(
            List<ExportFormat> enabledFormats,

            ExcelSettings excel,

            CsvSettings csv,

            JsonSettings json,

            PdfSettings pdf
    ) {
        public Export {
            if (enabledFormats == null || enabledFormats.isEmpty()) {
                enabledFormats = List.of(ExportFormat.EXCEL, ExportFormat.CSV);
            }
        }

        public enum ExportFormat {
            EXCEL, CSV, JSON, PDF, XML
        }
    }

    /**
     * Настройки Excel экспорта
     */
    public record ExcelSettings(
            String templatePath,
            Boolean useTemplate,
            Boolean createComparisonSheet,
            Boolean createChartsSheet,
            Integer maxRowsPerSheet
    ) {}

    /**
     * Настройки CSV экспорта
     */
    public record CsvSettings(
            String delimiter,
            String encoding,
            Boolean includeHeaders,
            String dateFormat
    ) {}

    /**
     * Настройки JSON экспорта
     */
    public record JsonSettings(
            Boolean prettyPrint,
            Boolean includeNulls,
            String dateFormat
    ) {}

    /**
     * Настройки PDF экспорта
     */
    public record PdfSettings(
            String templatePath,
            String fontPath,
            Boolean includeCharts,
            String paperSize
    ) {}

    /**
     * Настройки безопасности
     */
    public record Security(
            Boolean enabled,

            ApiKeySettings apiKey,

            RateLimitSettings rateLimit,

            CorsSettings cors
    ) {}

    /**
     * Настройки API ключей
     */
    public record ApiKeySettings(
            Boolean enabled,
            String headerName,
            List<String> validKeys
    ) {}

    /**
     * Настройки ограничения частоты запросов
     */
    public record RateLimitSettings(
            Boolean enabled,
            Integer requestsPerMinute,
            Integer requestsPerHour
    ) {}

    /**
     * Настройки CORS
     */
    public record CorsSettings(
            Boolean enabled,
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            Long maxAge
    ) {}
}