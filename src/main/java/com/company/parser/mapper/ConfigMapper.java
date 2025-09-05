package com.company.parser.mapper;

import com.company.parser.model.domain.ParsingConfig;
import com.company.parser.model.domain.Site;
import com.company.parser.model.domain.SiteSelectors;
import com.company.parser.model.dto.SiteConfigDTO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Маппер для конфигураций
 */
@Component
public class ConfigMapper {

    /**
     * Конвертация Site в SiteConfigDTO
     */
    public SiteConfigDTO toDto(Site site) {
        if (site == null) return null;

        Map<String, String> selectors = new HashMap<>();
        if (site.getSelectors() != null) {
            selectors.put("listSelector", site.getSelectors().getListSelector());
            selectors.put("nameSelector", site.getSelectors().getNameSelector());
            selectors.put("priceSelector", site.getSelectors().getPriceSelector());
            selectors.put("urlSelector", site.getSelectors().getUrlSelector());
            selectors.put("nextPageSelector", site.getSelectors().getNextPageSelector());
            selectors.put("sizeSelector", site.getSelectors().getSizeSelector());

            if (site.getSelectors().getThicknessSelector() != null) {
                selectors.put("thicknessSelector", site.getSelectors().getThicknessSelector());
            }
            if (site.getSelectors().getSteelGradeSelector() != null) {
                selectors.put("steelGradeSelector", site.getSelectors().getSteelGradeSelector());
            }
            if (site.getSelectors().getLengthSelector() != null) {
                selectors.put("lengthSelector", site.getSelectors().getLengthSelector());
            }
        }

        Map<String, Object> config = new HashMap<>();
        if (site.getConfig() != null) {
            config.put("maxPages", site.getConfig().getMaxPages());
            config.put("maxRecords", site.getConfig().getMaxRecords());
            config.put("delayBetweenPages", site.getConfig().getDelayBetweenPages());
            config.put("fetchProductDetails", site.getConfig().isFetchProductDetails());
            config.put("timeout", site.getConfig().getTimeout());
        }

        return SiteConfigDTO.builder()
                .id(site.getId())
                .name(site.getName())
                .startUrl(site.getStartUrl())
                .active(site.isActive())
                .parserType(site.getParserType())
                .selectors(selectors)
                .headers(site.getHeaders())
                .config(config)
                .build();
    }

    /**
     * Конвертация списка Site в список SiteConfigDTO
     */
    public List<SiteConfigDTO> toDtoList(List<Site> sites) {
        if (sites == null) return List.of();

        return sites.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Конвертация SiteConfigDTO в Site
     */
    public Site toEntity(SiteConfigDTO dto) {
        if (dto == null) return null;

        SiteSelectors selectors = null;
        if (dto.getSelectors() != null) {
            selectors = SiteSelectors.builder()
                    .listSelector(dto.getSelectors().get("listSelector"))
                    .nameSelector(dto.getSelectors().get("nameSelector"))
                    .priceSelector(dto.getSelectors().get("priceSelector"))
                    .urlSelector(dto.getSelectors().get("urlSelector"))
                    .nextPageSelector(dto.getSelectors().get("nextPageSelector"))
                    .sizeSelector(dto.getSelectors().get("sizeSelector"))
                    .thicknessSelector(dto.getSelectors().get("thicknessSelector"))
                    .steelGradeSelector(dto.getSelectors().get("steelGradeSelector"))
                    .lengthSelector(dto.getSelectors().get("lengthSelector"))
                    .build();
        }

        ParsingConfig config = ParsingConfig.builder().build();
        if (dto.getConfig() != null) {
            config = ParsingConfig.builder()
                    .maxPages(getIntValue(dto.getConfig(), "maxPages", 100))
                    .maxRecords(getIntValue(dto.getConfig(), "maxRecords", 10000))
                    .delayBetweenPages(getLongValue(dto.getConfig(), "delayBetweenPages", 500L))
                    .fetchProductDetails(getBooleanValue(dto.getConfig(), "fetchProductDetails", false))
                    .timeout(getIntValue(dto.getConfig(), "timeout", 30))
                    .build();
        }

        return Site.builder()
                .id(dto.getId())
                .name(dto.getName())
                .startUrl(dto.getStartUrl())
                .active(dto.isActive())
                .parserType(dto.getParserType() != null ? dto.getParserType() : "GENERIC")
                .selectors(selectors)
                .headers(dto.getHeaders())
                .config(config)
                .build();
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}
