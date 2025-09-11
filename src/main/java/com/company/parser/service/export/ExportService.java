package com.company.parser.service.export;

import com.company.parser.model.domain.PriceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    /**
     * Экспорт всех данных
     */
    public void exportAll(List<PriceRecord> current, List<PriceRecord> previous) {
        log.info("Exporting {} current and {} previous records",
                current.size(), previous != null ? previous.size() : 0);
        // TODO: Implement Excel/CSV export
    }
}
