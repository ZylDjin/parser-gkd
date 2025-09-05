package com.company.parser.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO для передачи информации о цене
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDTO {

    @NotBlank(message = "Site ID is required")
    private String siteId;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String size;
    private String thickness;
    private String steelGrade;
    private String length;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String unit;
    private String url;
    private Instant timestamp;
}
