package com.company.parser.core;

import java.math.BigDecimal;

public record PriceVariant(BigDecimal price, boolean gost, boolean tu, String note) {}