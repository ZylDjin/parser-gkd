package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO для изменения цены
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangeDTO {

    private String siteId;
    private String siteName;
    private String productName;
    private String size;
    private String thickness;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private String changeType;
    private Instant changeDate;
    private String url;
}
