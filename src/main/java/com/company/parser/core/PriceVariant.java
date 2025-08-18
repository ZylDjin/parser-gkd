package com.company.parser.core;

import java.math.BigDecimal;

public final class PriceVariant {
    private final BigDecimal price;
    private final boolean gost;
    private final boolean tu;
    private final String source;

    public PriceVariant(BigDecimal price, boolean gost, boolean tu, String source) {
        this.price = price;
        this.gost = gost;
        this.tu = tu;
        this.source = source;
    }
    public BigDecimal price() { return price; }
    public boolean gost() { return gost; }
    public boolean tu() { return tu; }
    public String source() { return source; }
}
