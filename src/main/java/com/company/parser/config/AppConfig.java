package com.company.parser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.InputStream;

/**
 * Общие бины приложения.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Пул для параллельных задач парсинга (если понадобится).
     */
    @Bean
    public TaskExecutor parseExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("parse-");
        ex.initialize();
        return ex;
    }

    /**
     * Загружаем sizes.yml и строим доменную конфигурацию размеров/исключений/slug-режимов.
     */
    @Bean
    public SizesConfig sizesConfig(AppProperties props, ResourceLoader loader) throws Exception {
        Resource res = loader.getResource(props.getSizesResource());
        if (!res.exists()) {
            throw new IllegalStateException("sizes.yml not found at: " + props.getSizesResource());
        }
        try (InputStream is = res.getInputStream()) {
            SizesConfig cfg = SizesConfig.load(is);
            log.info("SizesConfig loaded. Baseline: {}, categories: {}", cfg.getBaselineCompetitor(), cfg.getCategories());
            return cfg;
        }
    }
}
