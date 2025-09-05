package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO с ценой на конкретном сайте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SitePriceDTO {

    private String siteId;
    private String siteName;
    private BigDecimal price;
    private String currency;
    private String unit;
    private BigDecimal difference;
    private BigDecimal differencePercent;
    private String url;
    private boolean available;
}
