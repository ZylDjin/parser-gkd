package com.company.parser.service.scraper;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import com.company.parser.parser.base.SiteParser;
import com.company.parser.repository.SiteConfigRepository;
import com.company.parser.service.parser.SiteParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private final SiteConfigRepository siteConfigRepository;
    private final SiteParserFactory parserFactory;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Скрапинг всех активных сайтов
     */
    public List<PriceRecord> scrapeAllSites() {
        List<Site> activeSites = siteConfigRepository.findByActiveTrue();

        if (activeSites.isEmpty()) {
            log.warn("No active sites found for scraping");
            return Collections.emptyList();
        }

        log.info("Starting scraping for {} active sites", activeSites.size());

        List<CompletableFuture<List<PriceRecord>>> futures = activeSites.stream()
                .map(site -> CompletableFuture.supplyAsync(() -> scrapeSite(site.getId()), executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<PriceRecord> allRecords = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        log.info("Scraping completed: {} total records from {} sites", allRecords.size(), activeSites.size());
        return allRecords;
    }

    /**
     * Скрапинг конкретного сайта
     */
    public List<PriceRecord> scrapeSite(String siteId) {
        log.info("Starting scraping for site: {}", siteId);

        Site site = siteConfigRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));

        if (!site.isActive()) {
            log.warn("Site {} is not active", siteId);
            return Collections.emptyList();
        }

        try {
            SiteParser parser = parserFactory.getParserForSite(siteId);
            List<PriceRecord> records = parser.parse(site);

            log.info("Scraped {} records from site {}", records.size(), siteId);
            return records;

        } catch (Exception e) {
            log.error("Failed to scrape site {}: {}", siteId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}