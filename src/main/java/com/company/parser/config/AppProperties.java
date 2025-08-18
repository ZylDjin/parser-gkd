package com.company.parser.config;

import com.company.parser.core.Competitor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Глобальные настройки приложения из application.yml (префикс app).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Куда писать report.xlsx по умолчанию. */
    private String exportPath = "report.xlsx";

    /** Где лежит sizes.yml: classpath:/config/sizes.yml или file:/... */
    private String sizesResource = "classpath:config/sizes.yml";

    /** Фильтр цен: минимальная допустимая цена (для sanity-check). */
    private BigDecimal minPrice = new BigDecimal("20000");

    /** Фильтр цен: максимальная допустимая цена (для sanity-check). */
    private BigDecimal maxPrice = new BigDecimal("300000");

    /** Список включённых конкурентов (порядок колонок в Excel). */
    private List<Competitor> competitorsEnabled = List.of(Competitor.DEMIDOV, Competitor.AGRUPP);

    /** Базовые URL по конкурентам (можно переопределить в sizes.yml на уровне конкурента/категории). */
    private Map<Competitor, String> competitorsBaseUrls = Map.of();

    // ----- getters/setters -----

    public String getExportPath() { return exportPath; }
    public void setExportPath(String exportPath) { this.exportPath = exportPath; }

    public String getSizesResource() { return sizesResource; }
    public void setSizesResource(String sizesResource) { this.sizesResource = sizesResource; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public List<Competitor> getCompetitorsEnabled() { return competitorsEnabled; }
    public void setCompetitorsEnabled(List<Competitor> competitorsEnabled) { this.competitorsEnabled = competitorsEnabled; }

    public Map<Competitor, String> getCompetitorsBaseUrls() { return competitorsBaseUrls; }
    public void setCompetitorsBaseUrls(Map<Competitor, String> competitorsBaseUrls) { this.competitorsBaseUrls = competitorsBaseUrls; }
}
