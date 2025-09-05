package com.company.parser.repository;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Репозиторий для работы с данными о ценах
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PriceRepository {

    private static final String DATA_DIR = "./data/prices";
    private static final String ARCHIVE_DIR = "./data/prices/archive";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PriceRecord> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        createDirectories();
        loadCache();
    }

    /**
     * Сохранение записей
     */
    public void saveAll(List<PriceRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // Обновление кэша
        records.forEach(record -> {
            String key = generateKey(record);
            cache.put(key, record);
        });

        // Сохранение в файл
        saveToFile(records);

        log.info("Saved {} price records", records.size());
    }

    /**
     * Получение всех записей
     */
    public List<PriceRecord> findAll() {
        return new ArrayList<>(cache.values());
    }

    /**
     * Получение записей по сайту
     */
    public List<PriceRecord> findBySiteId(String siteId) {
        return cache.values().stream()
                .filter(record -> siteId.equals(record.getSiteId()))
                .collect(Collectors.toList());
    }

    /**
     * Получение записей за дату
     */
    public List<PriceRecord> findByDate(LocalDate date) {
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return cache.values().stream()
                .filter(record -> {
                    Instant timestamp = record.getTimestamp();
                    return timestamp != null &&
                            !timestamp.isBefore(startOfDay) &&
                            timestamp.isBefore(endOfDay);
                })
                .collect(Collectors.toList());
    }

    /**
     * Поиск записи по ключу
     */
    public Optional<PriceRecord> findByKey(String siteId, String size, String thickness) {
        String key = generateKey(siteId, size, thickness);
        return Optional.ofNullable(cache.get(key));
    }

    /**
     * Загрузка последних данных из файла
     */
    public List<PriceRecord> loadLatest() {
        try {
            Path latestFile = findLatestFile();
            if (latestFile != null) {
                return loadFromFile(latestFile);
            }
        } catch (IOException e) {
            log.error("Failed to load latest data", e);
        }

        return new ArrayList<>();
    }

    /**
     * Архивирование старых данных
     */
    public void archiveOldData(int daysToKeep) {
        try {
            Instant cutoff = Instant.now().minusSeconds(daysToKeep * 24L * 3600L);

            Files.list(Paths.get(DATA_DIR))
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(this::archiveFile);

        } catch (IOException e) {
            log.error("Failed to archive old data", e);
        }
    }

    /**
     * Удаление записей старше указанной даты
     */
    public void deleteOlderThan(Instant timestamp) {
        cache.entrySet().removeIf(entry -> {
            PriceRecord record = entry.getValue();
            return record.getTimestamp() != null && record.getTimestamp().isBefore(timestamp);
        });
    }

    // === Private методы ===

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(ARCHIVE_DIR));
        } catch (IOException e) {
            log.error("Failed to create directories", e);
        }
    }

    private void loadCache() {
        List<PriceRecord> records = loadLatest();
        records.forEach(record -> {
            String key = generateKey(record);
            cache.put(key, record);
        });
        log.info("Loaded {} records into cache", cache.size());
    }

    private void saveToFile(List<PriceRecord> records) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        String fileName = String.format("prices_%s.json.gz", timestamp);
        Path filePath = Paths.get(DATA_DIR, fileName);

        try (OutputStream os = Files.newOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {

            objectMapper.writeValue(gzos, records);
            log.debug("Saved {} records to {}", records.size(), fileName);

        } catch (IOException e) {
            log.error("Failed to save data to file", e);
        }
    }

    private List<PriceRecord> loadFromFile(Path filePath) {
        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        try (InputStream is = Files.newInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(is)) {

            return objectMapper.readValue(gzis,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PriceRecord.class));

        } catch (IOException e) {
            log.error("Failed to load data from file: {}", filePath, e);
            return Collections.emptyList();
        }
    }

    private Path findLatestFile() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(DATA_DIR))) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json.gz"))
                    .max(Comparator.comparing(p -> {
                        try {
                            return Files.getLastModifiedTime(p);
                        } catch (IOException e) {
                            return null;
                        }
                    }))
                    .orElse(null);
        }
    }

    private void archiveFile(Path file) {
        try {
            Path archivePath = Paths.get(ARCHIVE_DIR, file.getFileName().toString());
            Files.move(file, archivePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archived file: {}", file.getFileName());
        } catch (IOException e) {
            log.error("Failed to archive file: {}", file, e);
        }
    }

    private String generateKey(PriceRecord record) {
        return generateKey(record.getSiteId(), record.getSize(), record.getThickness());
    }

    private String generateKey(String siteId, String size, String thickness) {
        return String.join("|",
                siteId != null ? siteId : "",
                size != null ? size : "",
                thickness != null ? thickness : ""
        );
    }
}
