package com.company.parser.service;


import com.company.parser.config.SizesConfig;
import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.SizeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Обёртка над SizesConfig: удобные методы для получения базового конкурента,
 * списка размеров и базовых URL'ов по категории/конкуренту.
 */
@Service
public class SizesService {

    private static final Logger log = LoggerFactory.getLogger(SizesService.class);

    private final SizesConfig sizesConfig;

    public SizesService(SizesConfig sizesConfig) {
        this.sizesConfig = sizesConfig;
    }

    /** Базовый конкурент для категории (обычно DEMIDOV). */
    public Competitor baseline(Category category) {
        var b = sizesConfig.baseline(category);
        log.debug("[SizesService] baseline {} -> {}", category, b);
        return b;
    }

    /** Список размеров для категории (общий список, если у конкурента нет оверрайда). */
    public List<SizeKey> sizes(Category category) {
        var list = sizesConfig.sizes(category);
        log.debug("[SizesService] sizes {} -> {}", category, list);
        return list;
    }

    /**
     * Список размеров с учётом оверрайда для конкретного конкурента.
     * Если в конфиге у конкурента задан свой список — берём его, иначе общий.
     */
    public List<SizeKey> sizes(Category category, Competitor competitor) {
        var overridden = sizesConfig.sizes(category, competitor);
        if (overridden != null && !overridden.isEmpty()) {
            log.debug("[SizesService] sizes {} / {} (override) -> {}", category, competitor, overridden);
            return overridden;
        }
        return sizes(category);
    }

    /** Базовый URL конкурента по категории (если не задан, вернётся null — парсер возьмёт из AppProperties). */
    public String baseUrl(Category category, Competitor competitor) {
        return sizesConfig.baseUrl(category, competitor);
    }

    /** Доступные в конфиге категории. */
    public Set<Category> categories() {
        return sizesConfig.categories();
    }
}