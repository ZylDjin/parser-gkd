package com.company.parser.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SizesConfig {
    private final Map<Competitor, Map<Category, List<SizeKey>>> map = new EnumMap<>(Competitor.class);
    private SizesConfig() {}

    // === ПУБЛИЧНЫЕ ФАБРИКИ ===
    public static SizesConfig load(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(br, "file:" + path.toAbsolutePath());
        }
    }

    public static SizesConfig loadFromResource(String resourcePath) throws IOException {
        // напр.: "config/sizes.conf"
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found on classpath: " + resourcePath);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return parse(br, "cp:" + resourcePath);
            }
        }
    }

    // === ОБЩИЙ ПАРСЕР ===
    private static SizesConfig parse(BufferedReader br, String origin) throws IOException {
        SizesConfig cfg = new SizesConfig();
        String line; int lineno = 0;
        Competitor curComp = null; Category curCat = null;

        while ((line = br.readLine()) != null) {
            lineno++;
            String raw = line.trim();
            if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith(";") || raw.startsWith("//")) continue;

            if (raw.startsWith("[") && raw.endsWith("]")) {
                String[] parts = raw.substring(1, raw.length()-1).trim().split("\\.");
                if (parts.length != 2) throw new IOException(origin+": bad section at line "+lineno+": "+raw);
                try { curComp = Competitor.valueOf(parts[0].trim()); }
                catch (IllegalArgumentException e) { throw new IOException(origin+": unknown competitor at line "+lineno+": "+parts[0]); }
                try { curCat  = Category.valueOf(parts[1].trim()); }
                catch (IllegalArgumentException e) { throw new IOException(origin+": unknown category at line "+lineno+": "+parts[1]); }

                cfg.map.computeIfAbsent(curComp, k -> new EnumMap<>(Category.class))
                        .computeIfAbsent(curCat, k -> new ArrayList<>());
                continue;
            }

            if (curComp == null || curCat == null)
                throw new IOException(origin+": size without section at line "+lineno+": "+raw);

            SizeKey size = SizeKey.parse(raw);
            cfg.map.get(curComp).get(curCat).add(size);
        }
        return cfg;
    }

    // === АПИ ===
    public List<SizeKey> sizesFor(Competitor comp, Category cat) {
        return map.getOrDefault(comp, Map.of()).getOrDefault(cat, List.of());
    }
    public List<SizeKey> sizesUnion(Category cat) {
        Map<String, SizeKey> uniq = new LinkedHashMap<>();
        for (var byCat : map.values()) {
            var list = byCat.get(cat); if (list == null) continue;
            for (SizeKey s : list) uniq.putIfAbsent(s.toString(), s);
        }
        return new ArrayList<>(uniq.values());
    }
    public Set<Competitor> competitorsForCategory(Category cat) {
        Set<Competitor> out = new LinkedHashSet<>();
        for (var e : map.entrySet()) {
            if (!e.getValue().getOrDefault(cat, List.of()).isEmpty()) out.add(e.getKey());
        }
        return out;
    }
    public boolean hasSize(Competitor comp, Category cat, SizeKey size) {
        for (SizeKey s : sizesFor(comp, cat)) if (s.toString().equals(size.toString())) return true;
        return false;
    }
}
