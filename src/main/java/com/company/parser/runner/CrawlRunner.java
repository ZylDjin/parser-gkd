package com.company.parser.runner;

import com.company.parser.config.AppProperties;
import com.company.parser.core.*;
import com.company.parser.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CrawlRunner implements CommandLineRunner {

    private final AppProperties props;
    private final SizesService sizesService;
    private final SnapshotService snapshotService;
    private final CrawlService crawlService;
    private final ExportService exportService;

    public CrawlRunner(AppProperties props,
                       SizesService sizesService,
                       SnapshotService snapshotService,
                       CrawlService crawlService,
                       ExportService exportService) {
        this.props = props;
        this.sizesService = sizesService;
        this.snapshotService = snapshotService;
        this.crawlService = crawlService;
        this.exportService = exportService;
    }

    @Override
    public void run(String... args) throws Exception {
        Category category = Category.SP;

        SizesConfig cfg = sizesService.load(props.getSizesResource());
        var sizes = sizesService.sizesUnion(cfg, category);
        if (sizes.isEmpty()) {
            System.out.println("sizes.yml не содержит размеров для " + category);
            return;
        }

        // включённые конкуренты
        Set<Competitor> active = sizesService.enabledCompetitors(props.getCompetitorsEnabled());

        Snapshot prev = snapshotService.loadPrev();

        // основной сбор
        var rows = crawlService.crawl(category, sizes, active, cfg, prev);

        // снимок текущих new-цен
        Snapshot cur = new Snapshot();
        int foundNew = 0;
        for (var row : rows) {
            for (var e : row.newPrice.entrySet()) {
                if (e.getValue() != null) {
                    cur.put(category, row.size, e.getKey(), e.getValue());
                    foundNew++;
                }
            }
        }
        snapshotService.saveCur(cur);
        System.out.println("Saved snapshot_cur.csv");
        if (foundNew > 0) {
            snapshotService.advanceBaseline();
            System.out.println("Baseline advanced: snapshot_prev.csv updated.");
        } else {
            System.out.println("Baseline NOT advanced (no new prices found).");
        }

        var out = exportService.exportMatrix(category, rows, props.getExportPath());
        System.out.println("Excel report written: " + out.toAbsolutePath());
        System.out.println("Done.");
    }
}