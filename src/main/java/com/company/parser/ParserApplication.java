package com.company.parser;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения Parser GKD
 *
 * Система парсинга и анализа цен для мониторинга изменений
 * на различных сайтах поставщиков
 */
@Slf4j
@SpringBootApplication
@EnableScheduling // Включаем планировщик задач
@EnableAsync // Включаем асинхронное выполнение
@EnableCaching // Включаем кэширование
@EnableRetry // Включаем механизм повторных попыток
@EnableAspectJAutoProxy // Включаем AOP
@EnableSchedulerLock(defaultLockAtMostFor = "10m") // Распределенная блокировка для scheduled задач
@ConfigurationPropertiesScan("com.company.parser.config") // Сканирование конфигурационных классов
public class ParserApplication {

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(ParserApplication.class);

            // Настройка дополнительных параметров
            app.setLazyInitialization(false); // Отключаем ленивую инициализацию для production

            var context = app.run(args);

            log.info("""
                    
                    =====================================================
                    🚀 PARSER GKD STARTED SUCCESSFULLY
                    =====================================================
                    Application: {}
                    Version: {}
                    Profile(s): {}
                    Java: {}
                    =====================================================
                    """,
                    context.getEnvironment().getProperty("spring.application.name", "Parser GKD"),
                    context.getEnvironment().getProperty("application.version", "2.0.0"),
                    context.getEnvironment().getActiveProfiles().length > 0 ?
                            String.join(", ", context.getEnvironment().getActiveProfiles()) : "default",
                    System.getProperty("java.version")
            );

        } catch (Exception e) {
            log.error("Failed to start Parser Application", e);
            System.exit(1);
        }
    }
}