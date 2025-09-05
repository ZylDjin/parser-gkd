package com.company.parser.mapper;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.dto.*;
import com.company.parser.util.PriceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Маппер для создания отчетов
 */
@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final PriceUtils priceUtils;
    private final PriceMapper priceMapper;

    /**
     * Создание ComparisonDTO для сравнения цен
     */
    public ComparisonDTO toComparisonDto(String size, String thickness,
                                         Map<String, PriceRecord> pricesBySite,
                                         String baseSiteId) {

        PriceRecord baseRecord = pricesBySite.get(baseSiteId);
        BigDecimal basePrice = baseRecord != null ? baseRecord.getPrice() : null;

        List<SitePriceDTO> sitePrices = pricesBySite.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(baseSiteId))
                .map(entry -> createSitePriceDto(entry.getValue(), basePrice))
                .sorted(Comparator.comparing(SitePriceDTO::getPrice))
                .toList();

        // Расчет min/max
        List<BigDecimal> allPrices = pricesBySite.values().stream()
                .map(PriceRecord::getPrice)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal minPrice = allPrices.stream().min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxPrice = allPrices.stream().max(BigDecimal::compareTo).orElse(null);

        BigDecimal priceRange = null;
        BigDecimal priceRangePercent = null;

        if (minPrice != null && maxPrice != null) {
            priceRange = maxPrice.subtract(minPrice);
            if (minPrice.compareTo(BigDecimal.ZERO) > 0) {
                priceRangePercent = priceUtils.calculateChangePercent(minPrice, maxPrice);
            }
        }

        return ComparisonDTO.builder()
                .size(size)
                .thickness(thickness)
                .steelGrade(baseRecord != null ? baseRecord.getSteelGrade() : null)
                .basePrice(basePrice)
                .baseSiteId(baseSiteId)
                .baseSiteName(baseRecord != null ? baseRecord.getSiteName() : null)
                .sitePrices(sitePrices)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .priceRange(priceRange)
                .priceRangePercent(priceRangePercent)
                .build();
    }

    /**
     * Создание SitePriceDTO
     */
    private SitePriceDTO createSitePriceDto(PriceRecord record, BigDecimal basePrice) {
        BigDecimal difference = null;
        BigDecimal differencePercent = null;

        if (basePrice != null && record.getPrice() != null) {
            difference = record.getPrice().subtract(basePrice);
            differencePercent = priceUtils.calculateChangePercent(basePrice, record.getPrice());
        }

        return SitePriceDTO.builder()
                .siteId(record.getSiteId())
                .siteName(record.getSiteName())
                .price(record.getPrice())
                .currency(record.getCurrency())
                .unit(record.getUnit())
                .difference(difference)
                .differencePercent(differencePercent)
                .url(record.getUrl())
                .available(true)
                .build();
    }

    /**
     * Создание StatisticsDTO
     */
    public StatisticsDTO toStatisticsDto(List<PriceRecord> records,
                                         List<PriceChangeDTO> changes) {

        // Группировка по сайтам
        Map<String, Integer> recordsBySite = records.stream()
                .collect(Collectors.groupingBy(
                        PriceRecord::getSiteId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Уникальные размеры
        int uniqueSizes = (int) records.stream()
                .map(PriceRecord::getSize)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Уникальные продукты
        int uniqueProducts = (int) records.stream()
                .map(r -> r.getSize() + "|" + r.getThickness() + "|" + r.getSteelGrade())
                .distinct()
                .count();

        // Статистика цен
        DoubleSummaryStatistics priceStats = records.stream()
                .map(PriceRecord::getPrice)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .summaryStatistics();

        // Статистика изменений
        int priceIncreases = 0;
        int priceDecreases = 0;
        int newProducts = 0;
        int removedProducts = 0;

        if (changes != null) {
            for (PriceChangeDTO change : changes) {
                switch (change.getChangeType()) {
                    case "INCREASE", "CRITICAL_INCREASE" -> priceIncreases++;
                    case "DECREASE", "CRITICAL_DECREASE" -> priceDecreases++;
                    case "NEW" -> newProducts++;
                    case "REMOVED" -> removedProducts++;
                }
            }
        }

        return StatisticsDTO.builder()
                .totalRecords(records.size())
                .recordsBySite(recordsBySite)
                .uniqueProducts(uniqueProducts)
                .uniqueSizes(uniqueSizes)
                .averagePrice(BigDecimal.valueOf(priceStats.getAverage()))
                .minPrice(BigDecimal.valueOf(priceStats.getMin()))
                .maxPrice(BigDecimal.valueOf(priceStats.getMax()))
                .priceIncreases(priceIncreases)
                .priceDecreases(priceDecreases)
                .newProducts(newProducts)
                .removedProducts(removedProducts)
                .build();
    }

    /**
     * Создание ReportDTO
     */
    public ReportDTO toReportDto(String title,
                                 StatisticsDTO statistics,
                                 List<ComparisonDTO> comparisons,
                                 List<PriceChangeDTO> changes) {

        return ReportDTO.builder()
                .title(title)
                .generatedAt(Instant.now())
                .statistics(statistics)
                .comparisons(comparisons)
                .priceChanges(changes)
                .build();
    }
}
