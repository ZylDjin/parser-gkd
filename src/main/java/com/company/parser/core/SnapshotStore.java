package com.company.parser.core;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class SnapshotStore {
    private final Path file;

    public SnapshotStore(Path file) { this.file = file; }

    public Snapshot load() {
        Snapshot s = new Snapshot();
        if (!file.toFile().exists()) return s;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", -1);
                if (p.length != 4) continue;
                Category c = Category.valueOf(p[0]);
                SizeKey size = SizeKey.parse(p[1]);
                Competitor comp = Competitor.valueOf(p[2]);
                BigDecimal price = new BigDecimal(p[3]);
                s.put(c, size, comp, price);
            }
        } catch (IOException ignored) {}
        return s;
    }

    public void save(Snapshot s) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file.toFile()), StandardCharsets.UTF_8))) {
            for (var e : s.data().entrySet()) {
                String[] parts = e.getKey().split("\\|");
                String line = parts[0] + ";" + parts[1] + ";" + parts[2] + ";" + e.getValue();
                bw.write(line);
                bw.newLine();
            }
        }
    }
}
