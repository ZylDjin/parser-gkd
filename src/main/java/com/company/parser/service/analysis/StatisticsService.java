package com.company.parser.service.analysis;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import com.company.parser.model.dto.SiteResultDTO;
import com.company.parser.model.dto.StatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /**
     * Расчет статистики для списка записей
     */
    public StatisticsDTO calculateStatistics(List<PriceRecord> records, Site site) {
        if (records == null || records.isEmpty()) {
            return StatisticsDTO.builder()
                    .totalRecords(0)
                    .build();
        }

        // Группировка по сайтам
        Map<String, Integer> recordsBySite = records.stream()
                .collect(Collectors.groupingBy(
                        PriceRecord::getSiteId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Статистика цен
        DoubleSummaryStatistics priceStats = records.stream()
                .map(PriceRecord::getPrice)
                .filter(price -> price != null)
                .mapToDouble(BigDecimal::doubleValue)
                .summaryStatistics();

        // Уникальные продукты
        int uniqueProducts = (int) records.stream()
                .map(PriceRecord::getUniqueKey)
                .distinct()
                .count();

        // Уникальные размеры
        int uniqueSizes = (int) records.stream()
                .map(PriceRecord::getSize)
                .filter(size -> size != null && !size.isBlank())
                .distinct()
                .count();

        return StatisticsDTO.builder()
                .totalRecords(records.size())
                .recordsBySite(recordsBySite)
                .uniqueProducts(uniqueProducts)
                .uniqueSizes(uniqueSizes)
                .averagePrice(BigDecimal.valueOf(priceStats.getAverage()).setScale(2, RoundingMode.HALF_UP))
                .minPrice(BigDecimal.valueOf(priceStats.getMin()))
                .maxPrice(BigDecimal.valueOf(priceStats.getMax()))
                .build();
    }

    /**
     * Расчет статистики с учетом предыдущих данных
     */
    public StatisticsDTO calculateStatistics(List<PriceRecord> current, List<PriceRecord> previous) {
        StatisticsDTO stats = calculateStatistics(current, (Site) null);

        if (previous != null && !previous.isEmpty()) {
            // Дополняем статистику сравнением
            stats = enrichWithComparison(stats, current, previous);
        }

        return stats;
    }

    /**
     * Расчет общей статистики по результатам парсинга
     */
    public StatisticsDTO calculateOverallStatistics(List<SiteResultDTO> siteResults) {
        int totalRecords = siteResults.stream()
                .mapToInt(SiteResultDTO::getRecordsCount)
                .sum();

        Map<String, Integer> recordsBySite = siteResults.stream()
                .collect(Collectors.toMap(
                        SiteResultDTO::getSiteId,
                        SiteResultDTO::getRecordsCount
                ));

        return StatisticsDTO.builder()
                .totalRecords(totalRecords)
                .recordsBySite(recordsBySite)
                .build();
    }

    private StatisticsDTO enrichWithComparison(StatisticsDTO stats,
                                               List<PriceRecord> current,
                                               List<PriceRecord> previous) {
        // Создаем карты для сравнения
        var currentMap = current.stream()
                .collect(Collectors.toMap(
                        PriceRecord::getUniqueKey,
                        r -> r,
                        (r1, r2) -> r1
                ));

        var previousMap = previous.stream()
                .collect(Collectors.toMap(
                        PriceRecord::getUniqueKey,
                        r -> r,
                        (r1, r2) -> r1
                ));

        int priceIncreases = 0;
        int priceDecreases = 0;
        int newProducts = 0;
        int removedProducts = 0;

        // Считаем изменения
        for (var entry : currentMap.entrySet()) {
            PriceRecord prev = previousMap.get(entry.getKey());
            if (prev == null) {
                newProducts++;
            } else {
                int comparison = entry.getValue().getPrice().compareTo(prev.getPrice());
                if (comparison > 0) priceIncreases++;
                else if (comparison < 0) priceDecreases++;
            }
        }

        // Считаем удаленные продукты
        for (String key : previousMap.keySet()) {
            if (!currentMap.containsKey(key)) {
                removedProducts++;
            }
        }

        stats.setPriceIncreases(priceIncreases);
        stats.setPriceDecreases(priceDecreases);
        stats.setNewProducts(newProducts);
        stats.setRemovedProducts(removedProducts);

        return stats;
    }
}
