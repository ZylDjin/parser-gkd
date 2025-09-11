package com.company.parser.service.persistence;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.repository.HistoryRepository;
import com.company.parser.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final PriceRepository priceRepository;
    private final HistoryRepository historyRepository;

    /**
     * Сохранение данных
     */
    public int saveData(List<PriceRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        try {
            // Получаем предыдущие данные для истории
            List<PriceRecord> previousData = priceRepository.loadLatest();

            // Сохраняем новые данные
            priceRepository.saveAll(records);

            // Сохраняем историю изменений
            saveHistory(records, previousData);

            log.info("Saved {} records", records.size());
            return records.size();

        } catch (Exception e) {
            log.error("Failed to save data", e);
            throw new RuntimeException("Failed to persist data: " + e.getMessage(), e);
        }
    }

    /**
     * Загрузка последних данных
     */
    public List<PriceRecord> loadLatestData() {
        return priceRepository.loadLatest();
    }

    /**
     * Сохранение истории изменений
     */
    private void saveHistory(List<PriceRecord> current, List<PriceRecord> previous) {
        if (previous.isEmpty()) {
            return;
        }

        // Создаем карту предыдущих записей для быстрого поиска
        var previousMap = previous.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PriceRecord::getUniqueKey,
                        r -> r,
                        (r1, r2) -> r1
                ));

        // Сравниваем и сохраняем изменения
        for (PriceRecord currentRecord : current) {
            PriceRecord previousRecord = previousMap.get(currentRecord.getUniqueKey());

            if (previousRecord != null &&
                    !currentRecord.getPrice().equals(previousRecord.getPrice())) {
                historyRepository.addRecord(currentRecord, previousRecord);
            }
        }
    }
}
