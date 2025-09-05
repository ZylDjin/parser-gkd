package com.company.parser.model.enums;

/**
 * Поддерживаемые валюты
 */
public enum Currency {
    RUB("₽", "Российский рубль"),
    USD("$", "Доллар США"),
    EUR("€", "Евро"),
    CNY("¥", "Китайский юань"),
    GBP("£", "Британский фунт");

    private final String symbol;
    private final String description;

    Currency(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Получение валюты по коду
     */
    public static Currency fromCode(String code) {
        if (code == null) return RUB;

        try {
            return Currency.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RUB; // По умолчанию рубли
        }
    }
}
