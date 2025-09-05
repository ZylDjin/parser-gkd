package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Утилиты для работы с ценами
 */
@Component
public class PriceUtils {

    private static final DecimalFormat PRICE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru"));
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        PRICE_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Расчет процента изменения цены
     */
    public BigDecimal calculateChangePercent(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null ||
                oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Расчет абсолютного изменения
     */
    public BigDecimal calculateChangeAmount(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null) {
            return BigDecimal.ZERO;
        }
        return newPrice.subtract(oldPrice);
    }

    /**
     * Форматирование цены для отображения
     */
    public String formatPrice(BigDecimal price) {
        if (price == null) return "—";
        return PRICE_FORMAT.format(price);
    }

    /**
     * Форматирование цены с валютой
     */
    public String formatPrice(BigDecimal price, String currency) {
        if (price == null) return "—";

        String formatted = PRICE_FORMAT.format(price);
        String symbol = getCurrencySymbol(currency);

        return formatted + " " + symbol;
    }

    /**
     * Получение символа валюты
     */
    public String getCurrencySymbol(String currency) {
        if (currency == null) return "";

        return switch (currency.toUpperCase()) {
            case "RUB" -> "₽";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "CNY" -> "¥";
            case "GBP" -> "£";
            default -> currency;
        };
    }

    /**
     * Проверка валидности цены
     */
    public boolean isValidPrice(BigDecimal price) {
        if (price == null) return false;

        return price.compareTo(BigDecimal.ZERO) > 0 &&
                price.compareTo(new BigDecimal("10000000")) < 0;
    }

    /**
     * Округление цены
     */
    public BigDecimal roundPrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}

