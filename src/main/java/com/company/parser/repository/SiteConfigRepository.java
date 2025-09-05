package com.company.parser.repository;

import com.company.parser.config.AppProperties;
import com.company.parser.model.domain.Site;
import com.company.parser.model.domain.SiteSelectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Репозиторий для конфигураций сайтов
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SiteConfigRepository {

    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, Site> sitesCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadSites();
    }

    /**
     * Получение всех сайтов
     */
    @Cacheable("sites")
    public List<Site> findAll() {
        return new ArrayList<>(sitesCache.values());
    }

    /**
     * Получение активных сайтов
     */
    public List<Site> findByActiveTrue() {
        return sitesCache.values().stream()
                .filter(Site::isActive)
                .toList();
    }

    /**
     * Поиск сайта по ID
     */
    public Optional<Site> findById(String siteId) {
        return Optional.ofNullable(sitesCache.get(siteId));
    }

    /**
     * Сохранение сайта
     */
    public Site save(Site site) {
        if (site.getId() == null) {
            site.setId(UUID.randomUUID().toString());
        }
        sitesCache.put(site.getId(), site);
        saveSites();
        return site;
    }

    /**
     * Удаление сайта
     */
    public void deleteById(String siteId) {
        sitesCache.remove(siteId);
        saveSites();
    }

    /**
     * Перезагрузка конфигураций
     */
    public void reload() {
        sitesCache.clear();
        loadSites();
        log.info("Site configurations reloaded");
    }

    // === Private методы ===

    private void loadSites() {
        try {
            String sitesPath = appProperties.yaml().sites();
            Resource resource = resolveResource(sitesPath);

            if (!resource.exists()) {
                log.warn("Sites configuration not found: {}", sitesPath);
                loadDefaultSites();
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                SitesConfig config = yamlMapper.readValue(is, SitesConfig.class);

                if (config != null && config.sites != null) {
                    config.sites.forEach(site -> sitesCache.put(site.getId(), site));
                    log.info("Loaded {} site configurations", sitesCache.size());
                }
            }

        } catch (IOException e) {
            log.error("Failed to load sites configuration", e);
            loadDefaultSites();
        }
    }

    private void loadDefaultSites() {
        // Загрузка конфигураций по умолчанию
        Site demidov = Site.builder()
                .id("demidov")
                .name("Demidov Steel")
                .startUrl("https://demidovsteel.ru/catalog/truby-profilnye/")
                .active(true)
                .parserType("CUSTOM")
                .defaultCurrency("RUB")
                .defaultUnit("шт")
                .selectors(SiteSelectors.builder()
                        .listSelector(".catalog__list .catalog-item")
                        .nameSelector(".catalog-item__title")
                        .priceSelector(".price__current")
                        .urlSelector("a[href]")
                        .nextPageSelector("a[rel=next]")
                        .sizeSelector(".catalog-item__props")
                        .build())
                .build();

        Site agmarket = Site.builder()
                .id("agmarket")
                .name("AG Market")
                .startUrl("https://ag.market/catalog/truby-stalnye/truby-profilnye/")
                .active(true)
                .parserType("CUSTOM")
                .defaultCurrency("RUB")
                .defaultUnit("шт")
                .selectors(SiteSelectors.builder()
                        .listSelector(".products-list .product-card")
                        .nameSelector(".product-card__title")
                        .priceSelector(".product-card__price")
                        .urlSelector("a[href]")
                        .nextPageSelector(".pagination__next")
                        .sizeSelector(".product-card__specs")
                        .build())
                .build();

        sitesCache.put(demidov.getId(), demidov);
        sitesCache.put(agmarket.getId(), agmarket);

        log.info("Loaded {} default site configurations", sitesCache.size());
    }

    private void saveSites() {
        // Сохранение в файл (если нужно)
        log.debug("Sites saved to cache");
    }

    private Resource resolveResource(String location) {
        if (location.startsWith("classpath:") ||
                location.startsWith("file:") ||
                location.startsWith("http:")) {
            return resourceLoader.getResource(location);
        }

        Resource fileResource = resourceLoader.getResource("file:" + location);
        if (fileResource.exists()) {
            return fileResource;
        }

        return resourceLoader.getResource("classpath:" + location);
    }

    /**
     * Внутренний класс для десериализации YAML
     */
    private static class SitesConfig {
        public List<Site> sites;
    }
}
