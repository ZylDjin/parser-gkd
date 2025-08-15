package com.company.parser.core;

import java.math.BigDecimal;
import java.util.*;

public class ReportCalculator {

    public static class Row {
        public final SizeKey size;
        public final Map<Competitor, BigDecimal> oldPrice = new EnumMap<>(Competitor.class);
        public final Map<Competitor, BigDecimal> newPrice = new EnumMap<>(Competitor.class);
        public final Map<Competitor, BigDecimal> diff     = new EnumMap<>(Competitor.class); // стар - нов
        public BigDecimal minNew, avgNew, deltaFromOur;
        public Row(SizeKey size){ this.size = size; }
    }

    public Row calcRow(Category cat, SizeKey size,
                       List<? extends SiteParser> parsers,
                       PriceSelectorService selector,
                       Snapshot oldSnap,
                       Map<SizeKey, BigDecimal> ourPrice) throws Exception {
        Row r = new Row(size);

        // заполняем старые цены
        for (Competitor comp : Competitor.values()) {
            r.oldPrice.put(comp, oldSnap.get(cat, size, comp));
        }

        // получаем «новые» цены от парсеров и применяем правила выбора
        List<BigDecimal> newVals = new ArrayList<>();
        for (SiteParser sp : parsers) {
            var variants = sp.fetch(cat, size);
            BigDecimal chosen = selector.choose(variants);
            r.newPrice.put(sp.competitor(), chosen);
            if (chosen != null) newVals.add(chosen);
        }

        // diff = стар - нов
        for (Competitor comp : Competitor.values()) {
            BigDecimal o = r.oldPrice.get(comp);
            BigDecimal n = r.newPrice.get(comp);
            if (o != null && n != null) r.diff.put(comp, o.subtract(n));
        }

        // min/avg по новым
        r.minNew = newVals.stream().min(Comparator.naturalOrder()).orElse(null);
        if (!newVals.isEmpty()) {
            BigDecimal sum = newVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            r.avgNew = sum.divide(BigDecimal.valueOf(newVals.size()), BigDecimal.ROUND_HALF_UP);
        }

        // дельта от нашей = min(new) - our
        BigDecimal our = ourPrice.get(size);
        if (r.minNew != null && our != null) r.deltaFromOur = r.minNew.subtract(our);

        return r;
    }
}