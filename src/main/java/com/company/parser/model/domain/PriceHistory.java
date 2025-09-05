package com.company.parser.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * История изменения цены
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {

    /**
     * Идентификатор записи истории
     */
    private String id;

    /**
     * Идентификатор сайта
     */
    private String siteId;

    /**
     * Ключ продукта (size|thickness|steelGrade)
     */
    private String productKey;

    /**
     * Название продукта
     */
    private String productName;

    /**
     * Цена
     */
    private BigDecimal price;

    /**
     * Временная метка
     */
    private Instant timestamp;

    /**
     * Предыдущая цена
     */
    private BigDecimal previousPrice;

    /**
     * Изменение цены (абсолютное)
     */
    private BigDecimal priceChange;

    /**
     * Изменение цены (процент)
     */
    private BigDecimal priceChangePercent;

    /**
     * Тип изменения
     */
    private String changeType;

    /**
     * Расчет изменения цены
     */
    public void calculateChange() {
        if (previousPrice != null && price != null) {
            priceChange = price.subtract(previousPrice);
            if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
                priceChangePercent = priceChange
                        .divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }
    }
}
