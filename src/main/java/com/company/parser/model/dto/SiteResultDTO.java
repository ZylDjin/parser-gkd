package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * DTO с результатами парсинга конкретного сайта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteResultDTO {

    private String siteId;
    private String siteName;
    private boolean success;
    private int recordsCount;
    private int pagesProcessed;
    private Duration duration;
    private String errorMessage;
}
