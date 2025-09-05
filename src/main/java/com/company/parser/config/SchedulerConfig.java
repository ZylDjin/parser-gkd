package com.company.parser.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Конфигурация планировщика задач
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
@EnableSchedulerLock(defaultLockAtMostFor = "4h", defaultLockAtLeastFor = "1m")
public class SchedulerConfig implements SchedulingConfigurer {

    private final AppProperties appProperties;

    /**
     * Настройка планировщика задач
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
        log.info("Scheduler configured with cron expression: {}",
                appProperties.scheduler().cronExpression());
    }

    /**
     * Основной планировщик задач
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("parser-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            log.warn("Task rejected: {}", r.toString());
        });
        scheduler.initialize();

        log.info("Task scheduler initialized with pool size: 5");
        return scheduler;
    }

    /**
     * Альтернативный планировщик для виртуальных потоков (Java 21)
     */
    @Bean
    @ConditionalOnProperty(name = "app.scheduler.useVirtualThreads", havingValue = "true")
    public TaskScheduler virtualThreadTaskScheduler() {
        return new TaskSchedulerBuilder()
                .poolSize(10)
                .threadNamePrefix("virtual-scheduler-")
                .taskDecorator(runnable -> {
                    return () -> {
                        try {
                            log.debug("Starting scheduled task");
                            runnable.run();
                            log.debug("Scheduled task completed");
                        } catch (Exception e) {
                            log.error("Error in scheduled task", e);
                            throw e;
                        }
                    };
                })
                .build();
    }

    /**
     * ScheduledExecutorService для низкоуровневых задач
     */
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("scheduled-executor-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Lock Provider для распределенных блокировок (ShedLock)
     * Используется для предотвращения одновременного выполнения задач в кластере
     */
    @Bean
    @ConditionalOnProperty(name = "app.scheduler.enableLocking", havingValue = "true")
    public LockProvider lockProvider(DataSource dataSource) {
        // Создаем таблицу для блокировок если её нет
        createLockTableIfNotExists(dataSource);

        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Использовать время базы данных
                        .build()
        );
    }

    /**
     * Создание таблицы для ShedLock
     */
    private void createLockTableIfNotExists(DataSource dataSource) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            String createTableSql = """
                CREATE TABLE IF NOT EXISTS shedlock (
                    name VARCHAR(64) NOT NULL,
                    lock_until TIMESTAMP NOT NULL,
                    locked_at TIMESTAMP NOT NULL,
                    locked_by VARCHAR(255) NOT NULL,
                    PRIMARY KEY (name)
                )
                """;

            jdbcTemplate.execute(createTableSql);
            log.info("ShedLock table created or already exists");

        } catch (Exception e) {
            log.warn("Could not create ShedLock table, it might already exist: {}", e.getMessage());
        }
    }

    /**
     * Executor для асинхронных задач в рамках планировщика
     */
    @Bean(name = "schedulerExecutor")
    public Executor schedulerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Настройки тайм-аутов для задач
     */
    @Bean
    public SchedulerTimeoutConfig schedulerTimeoutConfig() {
        return SchedulerTimeoutConfig.builder()
                .defaultTimeout(Duration.ofHours(2))
                .parsingTimeout(Duration.ofHours(4))
                .notificationTimeout(Duration.ofMinutes(5))
                .exportTimeout(Duration.ofMinutes(30))
                .build();
    }

    /**
     * Конфигурация тайм-аутов
     */
    @lombok.Builder
    @lombok.Data
    public static class SchedulerTimeoutConfig {
        private Duration defaultTimeout;
        private Duration parsingTimeout;
        private Duration notificationTimeout;
        private Duration exportTimeout;
    }
}
