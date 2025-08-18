package com.company.parser.core;

import com.company.parser.core.parsers.SiteParser;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportCalculator {

    public static final class Row {
        public final SizeKey size;
        public final EnumMap<Competitor, BigDecimal> oldPrice = new EnumMap<>(Competitor.class);
        public final EnumMap<Competitor, BigDecimal> newPrice = new EnumMap<>(Competitor.class);
        public final EnumMap<Competitor, BigDecimal> diff     = new EnumMap<>(Competitor.class);
        public BigDecimal minNew, avgNew, deltaFromOur;

        public Row(SizeKey size) { this.size = size; }
    }

    public Row calcRow(Category cat, SizeKey size,
                       List<SiteParser> parsers,
                       PriceSelectorService selector,
                       Snapshot oldSnap,
                       Map<SizeKey, BigDecimal> ourPrice) throws Exception {

        Row r = new Row(size);

        // 1) читаем old из снапшота
        for (Competitor c : Competitor.values()) {
            BigDecimal old = oldSnap.get(cat, size, c);
            if (old != null) r.oldPrice.put(c, old);
        }

        // 2) собираем варианты new у каждого парсера
        for (SiteParser p : parsers) {
            var variants = p.fetch(cat, size);
            BigDecimal sel = selector.selectNewPrice(variants);
            if (sel != null) r.newPrice.put(p.competitor(), sel);
        }

        // 3) diff (new - old)
        for (Competitor c : Competitor.values()) {
            BigDecimal n = r.newPrice.get(c);
            BigDecimal o = r.oldPrice.get(c);
            if (n != null && o != null) r.diff.put(c, n.subtract(o));
        }

        // 4) агрегаты
        if (!r.newPrice.isEmpty()) {
            var values = r.newPrice.values().stream().filter(Objects::nonNull).toList();
            if (!values.isEmpty()) {
                r.minNew = values.stream().min(Comparator.naturalOrder()).orElse(null);
                r.avgNew = values.stream().mapToLong(BigDecimal::longValue).average().isPresent()
                        ? BigDecimal.valueOf(values.stream().mapToLong(BigDecimal::longValue).average().getAsDouble()).setScale(0, BigDecimal.ROUND_HALF_UP)
                        : null;
            }
        }

        // 5) delta(min - our) — чтобы не ломать старый консольный вывод (в Excel не используется)
        BigDecimal our = ourPrice.get(size);
        if (our != null && r.minNew != null) r.deltaFromOur = r.minNew.subtract(our);

        return r;
    }
}
