package com.company.parser.parser.base;

import com.company.parser.model.domain.Site;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Контекст парсинга для передачи данных между методами
 */
@Data
public class ParsingContext {

    private final Site site;
    private final Map<String, Object> attributes = new HashMap<>();
    private int currentPage = 1;
    private int totalRecords = 0;
    private long startTime = System.currentTimeMillis();

    public ParsingContext(Site site) {
        this.site = site;
    }

    /**
     * Добавление атрибута в контекст
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Получение атрибута из контекста
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * Получение атрибута с значением по умолчанию
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    /**
     * Увеличение счетчика страниц
     */
    public void incrementPage() {
        currentPage++;
    }

    /**
     * Добавление записей к общему количеству
     */
    public void addRecords(int count) {
        totalRecords += count;
    }

    /**
     * Получение времени выполнения в миллисекундах
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
