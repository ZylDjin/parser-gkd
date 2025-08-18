package com.company.parser.core;

import com.company.parser.config.AppProperties;
import com.company.parser.core.parsers.SiteParser;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Выбор «правильной» цены из вариантов, c фильтром адекватности и приоритетом ГОСТ. */
@Service
public class PriceSelectorService {

    private final int minPrice;
    private final int maxPrice;

    public PriceSelectorService(AppProperties props) {
        this.minPrice = props.getMinPrice();
        this.maxPrice = props.getMaxPrice();
    }

    public BigDecimal selectNewPrice(List<PriceVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;

        return variants.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.price() != null)
                .filter(v -> isSane(v.price()))
                // приоритет ГОСТ, затем минимальная цена
                .sorted(Comparator
                        .comparing(PriceVariant::gost).reversed()
                        .thenComparing(PriceVariant::price))
                .map(PriceVariant::price)
                .findFirst().orElse(null);
    }

    private boolean isSane(BigDecimal price) {
        int p = price.intValue();
        return p >= minPrice && p <= maxPrice;
    }
}
