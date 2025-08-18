package com.company.parser.service;

import com.company.parser.core.*;
import com.company.parser.core.parsers.SiteParser;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CrawlService {

    private final ParserRegistry registry;
    private final PriceSelectorService selector;
    private final ReportCalculator calculator;

    public CrawlService(ParserRegistry registry,
                        PriceSelectorService selector,
                        ReportCalculator calculator) {
        this.registry = registry;
        this.selector = selector;
        this.calculator = calculator;
    }

    public List<ReportCalculator.Row> crawl(Category cat,
                                            List<SizeKey> sizes,
                                            Set<Competitor> active,
                                            SizesConfig cfg,
                                            Snapshot prev) throws Exception {
        // «our» как baseline здесь не нужен (Matrix считает к Демидову), но
        // сигнатура calcRow требует — подаём пустую map.
        Map<SizeKey, BigDecimal> our = Collections.emptyMap();

        List<ReportCalculator.Row> rows = new ArrayList<>();

        for (SizeKey s : sizes) {
            List<SiteParser> parsers = new ArrayList<>();
            for (Competitor c : active) {
                if (cfg.hasSize(c, cat, s)) {
                    SiteParser sp = registry.get(c);
                    if (sp != null) parsers.add(sp);
                }
            }
            ReportCalculator.Row row = calculator.calcRow(cat, s, parsers, selector, prev, our);
            rows.add(row);
        }
        return rows;
    }
}
