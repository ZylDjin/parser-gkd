package com.company.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** Куда пишем Excel */
    private String exportPath = "report.xlsx";
    /** Откуда брать sizes.yml: classpath:..., file:... */
    private String sizesResource = "classpath:config/sizes.yml";
    /** Санити-фильтр цен */
    private int minPrice = 20000;
    private int maxPrice = 300000;
    /** Включённые конкуренты по enum-имени */
    private List<String> competitorsEnabled = List.of("AGRUPP", "DEMIDOV");
    /** BaseURL по конкурентам */
    private Map<String, String> competitorsBaseUrls;

    // getters/setters
    public String getExportPath() { return exportPath; }
    public void setExportPath(String exportPath) { this.exportPath = exportPath; }
    public String getSizesResource() { return sizesResource; }
    public void setSizesResource(String sizesResource) { this.sizesResource = sizesResource; }
    public int getMinPrice() { return minPrice; }
    public void setMinPrice(int minPrice) { this.minPrice = minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public void setMaxPrice(int maxPrice) { this.maxPrice = maxPrice; }
    public List<String> getCompetitorsEnabled() { return competitorsEnabled; }
    public void setCompetitorsEnabled(List<String> competitorsEnabled) { this.competitorsEnabled = competitorsEnabled; }
    public Map<String, String> getCompetitorsBaseUrls() { return competitorsBaseUrls; }
    public void setCompetitorsBaseUrls(Map<String, String> competitorsBaseUrls) { this.competitorsBaseUrls = competitorsBaseUrls; }
}
