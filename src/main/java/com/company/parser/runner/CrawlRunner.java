package com.company.parser.runner;

import com.company.parser.core.Category;
import com.company.parser.service.CrawlService;
import com.company.parser.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Запуск: парсинг Демидов + сравнение с AGRUPP, экспорт в MatrixT.
 * Убедись, что в application.yml включены DEMIDOV и AGRUPP,
 * а в competitorsBaseUrls задан URL Демидова.
 */
@Component
public class CrawlRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CrawlRunner.class);

    private final CrawlService crawlService;
    private final ExportService exportService;

    public CrawlRunner(CrawlService crawlService, ExportService exportService) {
        this.crawlService = crawlService;
        this.exportService = exportService;
    }

    @Override
    public void run(String... args) throws Exception {
        final Category category = Category.SP; // профильные трубы

        log.info("[CrawlRunner] Start crawl for {}", category);
        var snapshot = crawlService.crawl(category);

        var out = exportService.exportTransposedDefault(category, snapshot);
        log.info("[CrawlRunner] Excel report written: {}", out);
    }
}