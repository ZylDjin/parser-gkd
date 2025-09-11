package com.company.parser.repository;

import com.company.parser.model.domain.PriceHistory;
import com.company.parser.model.domain.PriceRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Репозиторий для истории изменений цен
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HistoryRepository {

    private static final String HISTORY_DIR = "./data/history";
    private static final String HISTORY_FILE = "price_history.json.gz";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<PriceHistory>> historyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        createDirectories();
        loadHistory();
    }

    /**
     * Сохранение истории изменения цены
     */
    public void save(PriceHistory history) {
        if (history == null) return;

        String key = generateKey(history.getSiteId(), history.getProductKey());
        historyCache.computeIfAbsent(key, k -> new ArrayList<>()).add(history);

        saveToFile();
    }

    /**
     * Добавление записи в историю
     */
    public void addRecord(PriceRecord current, PriceRecord previous) {
        PriceHistory history = createHistory(current, previous);
        save(history);
    }

    /**
     * Получение истории для продукта
     */
    public List<PriceHistory> findByProduct(String siteId, String productKey) {
        String key = generateKey(siteId, productKey);
        return historyCache.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Получение истории за период
     */
    public List<PriceHistory> findByPeriod(Instant from, Instant to) {
        return historyCache.values().stream()
                .flatMap(List::stream)
                .filter(h -> !h.getTimestamp().isBefore(from) && !h.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Получение последних изменений
     */
    public List<PriceHistory> findRecent(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        return historyCache.values().stream()
                .flatMap(List::stream)
                .filter(h -> h.getTimestamp().isAfter(cutoff))
                .sorted(Comparator.comparing(PriceHistory::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Очистка старой истории
     */
    public void cleanOldHistory(int daysToKeep) {
        Instant cutoff = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);

        historyCache.values().forEach(list ->
                list.removeIf(h -> h.getTimestamp().isBefore(cutoff))
        );

        // Удаление пустых записей
        historyCache.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        saveToFile();
        log.info("Cleaned history older than {} days", daysToKeep);
    }

    // === Private методы ===

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(HISTORY_DIR));
        } catch (IOException e) {
            log.error("Failed to create directories", e);
        }
    }

    private void loadHistory() {
        Path filePath = Paths.get(HISTORY_DIR, HISTORY_FILE);
        if (!Files.isRegularFile(filePath)) return;

        try (InputStream fis = Files.newInputStream(filePath);
             InputStream in  = isGzip(fis) ? new GZIPInputStream(fis) : fis) {

            var tf = objectMapper.getTypeFactory();
            var keyType   = tf.constructType(String.class);
            var valueType = tf.constructCollectionType(List.class, PriceHistory.class); // List<PriceHistory>
            var mapType   = tf.constructMapType(Map.class, keyType, valueType);        // Map<String, List<PriceHistory>>

            Map<String, List<PriceHistory>> loaded = objectMapper.readValue(in, mapType);
            if (loaded == null) loaded = Map.of();

            historyCache.clear();
            historyCache.putAll(loaded);
            log.info("Loaded price history for {} products", historyCache.size());

        } catch (IOException e) {
            log.error("Failed to load history from file {}", filePath.toAbsolutePath(), e);
        }
    }

    /** Проверяем сигнатуру GZIP (0x1F 0x8B), не закрывая поток. */
    private boolean isGzip(InputStream is) throws IOException {
        if (!is.markSupported()) is = new BufferedInputStream(is);
        is.mark(2);
        int b1 = is.read(), b2 = is.read();
        is.reset();
        return b1 == 0x1f && b2 == 0x8b;
    }

    private void saveToFile() {
        Path filePath = Paths.get(HISTORY_DIR, HISTORY_FILE);

        try (OutputStream os = Files.newOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {

            objectMapper.writeValue(gzos, historyCache);

        } catch (IOException e) {
            log.error("Failed to save history to file", e);
        }
    }

    private PriceHistory createHistory(PriceRecord current, PriceRecord previous) {
        PriceHistory history = PriceHistory.builder()
                .id(UUID.randomUUID().toString())
                .siteId(current.getSiteId())
                .productKey(generateProductKey(current))
                .productName(current.getProductName())
                .price(current.getPrice())
                .timestamp(current.getTimestamp())
                .previousPrice(previous != null ? previous.getPrice() : null)
                .build();

        history.calculateChange();

        if (previous != null) {
            if (current.getPrice().compareTo(previous.getPrice()) > 0) {
                history.setChangeType("INCREASE");
            } else if (current.getPrice().compareTo(previous.getPrice()) < 0) {
                history.setChangeType("DECREASE");
            } else {
                history.setChangeType("NO_CHANGE");
            }
        } else {
            history.setChangeType("NEW");
        }

        return history;
    }

    private String generateKey(String siteId, String productKey) {
        return siteId + "|" + productKey;
    }

    private String generateProductKey(PriceRecord record) {
        return String.join("|",
                record.getSize() != null ? record.getSize() : "",
                record.getThickness() != null ? record.getThickness() : "",
                record.getSteelGrade() != null ? record.getSteelGrade() : ""
        );
    }
}