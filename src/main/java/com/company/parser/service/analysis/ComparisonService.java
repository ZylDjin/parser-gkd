package com.company.parser.service.analysis;

import com.company.parser.model.domain.PriceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    /**
     * Генерация сравнений
     */
    public void generateComparisons(List<PriceRecord> current, List<PriceRecord> previous) {
        log.info("Generating comparisons for {} records", current.size());
        // TODO: Implement comparison logic
    }
}

