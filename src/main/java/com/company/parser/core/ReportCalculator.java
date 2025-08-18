package com.company.parser.core;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Собирает построчные данные из Snapshot для дальнейшего вывода/экспорта.
 * Совместим с текущим ExcelExporter (лист Matrix).
 */
@Service
public class ReportCalculator {

    /**
     * Одна строка отчёта по конкретному размеру.
     */
    public static final class Row {
        private final SizeKey size;

        /** baseline-конкурент (например, DEMIDOV) — его новая цена. */
        private final BigDecimal baselineNew;

        /** Цены по конкурента́м (кроме baseline): comp -> new. */
        private final Map<Competitor, BigDecimal> newByCompetitor;

        /** Дельта к baseline для каждого конкурента (new − baseline). */
        private final Map<Competitor, BigDecimal> deltaByCompetitor;

        /** Максимальная цена среди конкурентов (кроме baseline). */
        private final BigDecimal maxNew;

        /** Δ(MAX − baseline). */
        private final BigDecimal deltaMaxBaseline;

        public Row(SizeKey size,
                   BigDecimal baselineNew,
                   Map<Competitor, BigDecimal> newByCompetitor,
                   Map<Competitor, BigDecimal> deltaByCompetitor,
                   BigDecimal maxNew,
                   BigDecimal deltaMaxBaseline) {
            this.size = size;
            this.baselineNew = baselineNew;
            this.newByCompetitor = Collections.unmodifiableMap(new LinkedHashMap<>(newByCompetitor));
            this.deltaByCompetitor = Collections.unmodifiableMap(new LinkedHashMap<>(deltaByCompetitor));
            this.maxNew = maxNew;
            this.deltaMaxBaseline = deltaMaxBaseline;
        }

        public SizeKey getSize() { return size; }
        public BigDecimal getBaselineNew() { return baselineNew; }
        public Map<Competitor, BigDecimal> getNewByCompetitor() { return newByCompetitor; }
        public Map<Competitor, BigDecimal> getDeltaByCompetitor() { return deltaByCompetitor; }
        public BigDecimal getMaxNew() { return maxNew; }
        public BigDecimal getDeltaMaxBaseline() { return deltaMaxBaseline; }
    }

    /**
     * Строит строки для листа Matrix.
     *
     * @param category  категория (например, Category.SP)
     * @param sizes     список размеров (в нужном порядке строк)
     * @param enabled   конкуренты в порядке колонок (как в application.yml), включает baseline
     * @param snapshot  текущий снапшот (минимальные валидные цены по конкурентам)
     * @param baseline  baseline-конкурент (из SizesConfig)
     */
    public List<Row> buildRows(Category category,
                               List<SizeKey> sizes,
                               List<Competitor> enabled,
                               Snapshot snapshot,
                               Competitor baseline) {

        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(sizes, "sizes");
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(baseline, "baseline");

        // список «прочих» конкурентов (для колонок и MAX)
        List<Competitor> others = new ArrayList<>();
        for (Competitor c : enabled) if (c != baseline) others.add(c);

        List<Row> rows = new ArrayList<>(sizes.size());

        for (SizeKey size : sizes) {
            BigDecimal base = snapshot.get(category, size, baseline);

            Map<Competitor, BigDecimal> newBy = new LinkedHashMap<>();
            Map<Competitor, BigDecimal> deltaBy = new LinkedHashMap<>();

            BigDecimal max = null;
            for (Competitor comp : others) {
                BigDecimal p = snapshot.get(category, size, comp);
                newBy.put(comp, p);

                BigDecimal d = (p != null && base != null) ? p.subtract(base) : null;
                deltaBy.put(comp, d);

                if (p != null) {
                    max = (max == null || p.compareTo(max) > 0) ? p : max;
                }
            }

            BigDecimal dMax = (max != null && base != null) ? max.subtract(base) : null;

            rows.add(new Row(size, base, newBy, deltaBy, max, dMax));
        }

        return rows;
    }
}
