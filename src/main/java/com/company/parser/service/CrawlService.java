package com.company.parser.service;

import com.company.parser.core.*;
import com.company.parser.core.parsers.ParserRegistry;
import com.company.parser.core.parsers.SiteParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Выполняет обход сайтов по списку размеров и собирает "матрицу" цен:
 * Map<Competitor, Map<SizeKey, BigDecimal>>.
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);

    private final ParserRegistry registry;
    private final SizesService sizesService;
    private final PriceSelectorService selector;

    public CrawlService(ParserRegistry registry,
                        SizesService sizesService,
                        PriceSelectorService selector) {
        this.registry = registry;
        this.sizesService = sizesService;
        this.selector = selector;
    }

    /**
     * Основной метод обхода: для категории собираем цены по включённым конкурентам.
     * Возвращает результат, пригодный для экспорта в Excel.
     */
    public CrawlResult crawl(Category category) {
        List<SizeKey> sizes = sizesService.sizes(category); // общий список размеров
        Competitor baseline = sizesService.baseline(category);

        log.info("[CrawlService] category={} baseline={} sizes={}", category, baseline, sizes.size());

        // итоговая матрица: competitor -> (size -> price)
        Map<Competitor, Map<SizeKey, BigDecimal>> matrix = new EnumMap<>(Competitor.class);

        // сначала обойдём baseline (если включён), затем остальных — только для наглядных логов
        List<Competitor> order = new ArrayList<>();
        if (registry.isEnabled(baseline)) order.add(baseline);
        for (Competitor c : registry.enabledCompetitors()) {
            if (!c.equals(baseline)) order.add(c);
        }

        for (Competitor comp : order) {
            var parserOpt = registry.get(comp);
            if (parserOpt.isEmpty()) {
                log.warn("[CrawlService] no parser for {}", comp);
                continue;
            }
            SiteParser parser = parserOpt.get();

            // если у конкурента есть свой список размеров — используем его
            List<SizeKey> sizesForComp = sizesService.sizes(category, comp);
            log.info("[{}] sizes to crawl: {}", comp.title(), sizesForComp.size());

            Map<SizeKey, BigDecimal> prices = new TreeMap<>();
            for (SizeKey s : sizesForComp) {
                try {
                    List<PriceVariant> variants = parser.fetch(category, s);
                    var selected = selector.selectFromVariants(variants, comp, category, s);
                    if (selected.isPresent()) {
                        BigDecimal price = selected.get();
                        prices.put(s, price);
                        log.info("[{}] {} -> {}", comp.title(), s, price);
                    } else {
                        log.info("[{}] {} -> (no valid price)", comp.title(), s);
                    }
                } catch (Exception e) {
                    log.warn("[{}] {} -> ERROR: {}", comp.title(), s, e.toString());
                }
            }
            matrix.put(comp, prices);
        }

        return new CrawlResult(category, baseline, sizes, matrix);
    }

    /** Результат обхода: всё, что нужно экспортеру. */
    public static final class CrawlResult {
        private final Category category;
        private final Competitor baseline;
        private final List<SizeKey> sizes; // колонками
        private final Map<Competitor, Map<SizeKey, BigDecimal>> data; // строками

        public CrawlResult(Category category,
                           Competitor baseline,
                           List<SizeKey> sizes,
                           Map<Competitor, Map<SizeKey, BigDecimal>> data) {
            this.category = category;
            this.baseline = baseline;
            this.sizes = List.copyOf(sizes);
            this.data = data;
        }

        public Category category() { return category; }
        public Competitor baseline() { return baseline; }
        public List<SizeKey> sizes() { return sizes; }
        public Map<Competitor, Map<SizeKey, BigDecimal>> data() { return data; }
    }
}