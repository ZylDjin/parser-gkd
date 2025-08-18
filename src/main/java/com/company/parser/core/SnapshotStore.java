package com.company.parser.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Хранение снапшотов в CSV-файле UTF-8 с заголовком:
 *   category,size,competitor,price
 *
 * - Если файла нет при загрузке, возвращает пустой Snapshot.
 * - Строки с ошибками парсинга аккуратно пропускаются.
 */
public final class SnapshotStore {

    private static final String HEADER = "category,size,competitor,price";
    private final Path file;

    public SnapshotStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** Загрузить снапшот из CSV. Если файл отсутствует — вернуть пустой. */
    public Snapshot load() throws IOException {
        Snapshot snapshot = new Snapshot();
        if (!Files.exists(file)) return snapshot;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            // пропустим заголовок, если он есть
            if (line != null && !isHeader(line)) {
                // первая строка — тоже данные
                parseLineIntoSnapshot(line, snapshot);
            }
            // остальные строки
            while ((line = br.readLine()) != null) {
                parseLineIntoSnapshot(line, snapshot);
            }
        }
        return snapshot;
    }

    /** Сохранить снапшот в CSV, перезаписав файл. */
    public void save(Snapshot snapshot) throws IOException {
        Files.createDirectories(file.getParent() != null ? file.getParent() : file.toAbsolutePath().getParent());
        // Соберём, отсортируем для стабильности:
        List<Snapshot.Entry> entries = new ArrayList<>();
        for (Snapshot.Entry e : snapshot.entries()) entries.add(e);

        entries.sort(Comparator
                .comparing((Snapshot.Entry e) -> e.category().name())
                .thenComparing(e -> e.size().toString())
                .thenComparing(e -> e.competitor().name()));

        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            bw.write(HEADER);
            bw.newLine();
            for (Snapshot.Entry e : entries) {
                bw.write(e.category().name());
                bw.write(',');
                bw.write(e.size().toString());
                bw.write(',');
                bw.write(e.competitor().name());
                bw.write(',');
                bw.write(e.price().toPlainString());
                bw.newLine();
            }
        }
    }

    // ========== утилиты ==========

    private static boolean isHeader(String line) {
        return line.trim().equalsIgnoreCase(HEADER);
    }

    private static void parseLineIntoSnapshot(String raw, Snapshot snapshot) {
        if (raw == null) return;
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) return;

        // простейший CSV (без кавычек — наши поля простые)
        String[] parts = line.split(",", -1);
        if (parts.length < 4) return;

        String catStr  = parts[0].trim();
        String sizeStr = parts[1].trim();
        String compStr = parts[2].trim();
        String priceStr= parts[3].trim();

        try {
            Category cat = Category.valueOf(catStr);
            SizeKey size = SizeKey.parse(sizeStr);
            Competitor comp = Competitor.valueOf(compStr);
            BigDecimal price = new BigDecimal(priceStr);
            snapshot.put(cat, size, comp, price);
        } catch (Exception ignore) {
            // игнорируем кривые строки
        }
    }
}
