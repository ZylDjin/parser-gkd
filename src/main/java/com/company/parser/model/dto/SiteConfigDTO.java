package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/**
 * DTO для конфигурации сайта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigDTO {

    @NotBlank(message = "Site ID is required")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "Site ID must contain only lowercase letters, numbers, underscore and hyphen")
    private String id;

    @NotBlank(message = "Site name is required")
    private String name;

    @NotBlank(message = "Start URL is required")
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String startUrl;

    private boolean active;

    private String parserType;

    private Map<String, String> selectors;

    private Map<String, String> headers;

    private Map<String, Object> config;
}
