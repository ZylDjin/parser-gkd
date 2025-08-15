package com.company.parser.core;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class PriceSelectorService {
    public BigDecimal choose(List<PriceVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;

        List<PriceVariant> gost = variants.stream().filter(PriceVariant::gost).toList();
        if (!gost.isEmpty()) return midMinMax(gost);

        List<PriceVariant> tu = variants.stream().filter(PriceVariant::tu).toList();
        if (!tu.isEmpty()) return midMinMax(tu);

        return midMinMax(variants); // если меток нет — по всем
    }

    private static BigDecimal midMinMax(List<PriceVariant> list) {
        BigDecimal min = list.stream().map(PriceVariant::price).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = list.stream().map(PriceVariant::price).max(Comparator.naturalOrder()).orElse(null);
        return (min == null || max == null) ? null : min.add(max).divide(BigDecimal.valueOf(2));
    }
}
