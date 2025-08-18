package com.company.parser.core;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Конфиг размеров/конкурентов, загружаемый из YAML (sizes.yml).
 *
 * Поддерживает:
 *  - baselineCompetitor
 *  - competitors[COMP].enabled/baseUrl/url.sizeSlug
 *  - categories[CAT].all
 *  - categories[CAT].byCompetitor[COMP].include/exclude
 *
 * Основные методы, которыми пользуемся в сервисах:
 *  - sizesUnion(Category)  — объединённый список размеров для категории (all + все include),
 *                            НЕ вычитает exclude (exclude применяется точечно в hasSize).
 *  - hasSize(Competitor, Category, SizeKey) — учитывает include/exclude для конкурента.
 *  - getBaselineCompetitor()
 *  - getCompetitorBaseUrl(Competitor)
 *  - getSizeSlugMode(Competitor)
 */
public final class SizesConfig {

    // ==== публичное API ====

    public static SizesConfig load(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return load(is);
        }
    }

    public static SizesConfig load(InputStream is) {
        Objects.requireNonNull(is, "InputStream is null");
        Yaml yaml = new Yaml();
        var root = yaml.loadAs(new InputStreamReader(is, StandardCharsets.UTF_8), YamlRoot.class);
        if (root == null) throw new IllegalArgumentException("sizes.yml is empty or invalid");

        return fromYaml(root);
    }

    /** Объединённый список размеров для категории (all + все include). Уникальный, в порядке добавления. */
    public List<SizeKey> sizesUnion(Category cat) {
        CategoryBlock cb = categories.get(cat);
        if (cb == null) return List.of();

        LinkedHashSet<SizeKey> out = new LinkedHashSet<>(cb.all);
        if (cb.byCompetitor != null) {
            for (Overrides ov : cb.byCompetitor.values()) {
                if (ov.include != null) out.addAll(ov.include);
            }
        }
        return new ArrayList<>(out);
    }

    /** Должен ли конкурент обрабатывать данный размер в категории (учитывает include/exclude). */
    public boolean hasSize(Competitor comp, Category cat, SizeKey size) {
        CategoryBlock cb = categories.get(cat);
        if (cb == null) return false;
        // база: all
        LinkedHashSet<SizeKey> set = new LinkedHashSet<>(cb.all);
        // точечные правки по конкуренту
        Overrides ov = (cb.byCompetitor == null) ? null : cb.byCompetitor.get(comp);
        if (ov != null) {
            if (ov.include != null) set.addAll(ov.include);
            if (ov.exclude != null) set.removeAll(ov.exclude);
        }
        return set.contains(size);
    }

    public Competitor getBaselineCompetitor() {
        return baselineCompetitor;
    }

    public Optional<String> getCompetitorBaseUrl(Competitor c) {
        CompetitorCfg cfg = competitors.get(c);
        return (cfg != null && cfg.baseUrl != null && !cfg.baseUrl.isBlank())
                ? Optional.of(cfg.baseUrl) : Optional.empty();
    }

    public SizeSlugMode getSizeSlugMode(Competitor c) {
        CompetitorCfg cfg = competitors.get(c);
        return (cfg != null && cfg.url != null && cfg.url.sizeSlug != null)
                ? cfg.url.sizeSlug
                : SizeSlugMode.default_; // по умолчанию "AxBxT" (с точкой в толщине)
    }

    public boolean isCompetitorEnabled(Competitor c) {
        CompetitorCfg cfg = competitors.get(c);
        return cfg != null && Boolean.TRUE.equals(cfg.enabled);
    }

    // ==== внутренняя модель (после конверсии из YAML) ====

    public enum SizeSlugMode {
        default_,       // 40x20x1.5
        thicknessDash;  // 40x20x1-5 (как у AGRUPP)
    }

    private Competitor baselineCompetitor = Competitor.DEMIDOV;
    private final EnumMap<Competitor, CompetitorCfg> competitors = new EnumMap<>(Competitor.class);
    private final EnumMap<Category, CategoryBlock> categories = new EnumMap<>(Category.class);

    private static final class CompetitorCfg {
        Boolean enabled;
        String baseUrl;
        UrlCfg url;
    }
    private static final class UrlCfg {
        SizeSlugMode sizeSlug;
    }

    private static final class CategoryBlock {
        final LinkedHashSet<SizeKey> all = new LinkedHashSet<>();
        final EnumMap<Competitor, Overrides> byCompetitor = new EnumMap<>(Competitor.class);
    }
    private static final class Overrides {
        LinkedHashSet<SizeKey> include;
        LinkedHashSet<SizeKey> exclude;
    }

    // ==== конверсия из YAML-DTO в рабочую модель ====

    private static SizesConfig fromYaml(YamlRoot y) {
        SizesConfig cfg = new SizesConfig();

        // baseline
        if (y.baselineCompetitor != null && !y.baselineCompetitor.isBlank()) {
            cfg.baselineCompetitor = toCompetitor(y.baselineCompetitor)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown baselineCompetitor: " + y.baselineCompetitor));
        }

        // competitors
        if (y.competitors != null) {
            for (var e : y.competitors.entrySet()) {
                var c = toCompetitor(e.getKey()).orElse(null);
                if (c == null) continue; // игнорируем неизвестные
                var yc = e.getValue();
                CompetitorCfg cc = new CompetitorCfg();
                cc.enabled = yc.enabled;
                cc.baseUrl = yc.baseUrl;
                if (yc.url != null) {
                    UrlCfg uc = new UrlCfg();
                    if (yc.url.sizeSlug != null) {
                        uc.sizeSlug = toSlugMode(yc.url.sizeSlug)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown sizeSlug: " + yc.url.sizeSlug));
                    }
                    cc.url = uc;
                }
                cfg.competitors.put(c, cc);
            }
        }

        // categories
        if (y.categories == null || y.categories.isEmpty()) {
            throw new IllegalArgumentException("sizes.yml: 'categories' is empty");
        }
        for (var e : y.categories.entrySet()) {
            Category cat = toCategory(e.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + e.getKey()));
            var yc = e.getValue();

            CategoryBlock cb = new CategoryBlock();
            // all
            if (yc.all != null) {
                for (String s : yc.all) {
                    cb.all.add(parseSizeSafe(s));
                }
            }
            // byCompetitor
            if (yc.byCompetitor != null) {
                for (var be : yc.byCompetitor.entrySet()) {
                    Competitor comp = toCompetitor(be.getKey())
                            .orElseThrow(() -> new IllegalArgumentException("Unknown competitor in byCompetitor: " + be.getKey()));
                    var yov = be.getValue();

                    Overrides ov = new Overrides();
                    if (yov.include != null) {
                        ov.include = new LinkedHashSet<>();
                        for (String s : yov.include) ov.include.add(parseSizeSafe(s));
                    }
                    if (yov.exclude != null) {
                        ov.exclude = new LinkedHashSet<>();
                        for (String s : yov.exclude) ov.exclude.add(parseSizeSafe(s));
                    }
                    cb.byCompetitor.put(comp, ov);
                }
            }

            cfg.categories.put(cat, cb);
        }

        return cfg;
    }

    // ==== утилиты ====

    private static Optional<Competitor> toCompetitor(String key) {
        if (key == null) return Optional.empty();
        String k = key.trim().toUpperCase(Locale.ROOT);
        try { return Optional.of(Competitor.valueOf(k)); }
        catch (Exception e) { return Optional.empty(); }
    }

    private static Optional<Category> toCategory(String key) {
        if (key == null) return Optional.empty();
        String k = key.trim().toUpperCase(Locale.ROOT);
        try { return Optional.of(Category.valueOf(k)); }
        catch (Exception e) { return Optional.empty(); }
    }

    private static Optional<SizeSlugMode> toSlugMode(String key) {
        if (key == null) return Optional.empty();
        String k = key.trim().toLowerCase(Locale.ROOT);
        return switch (k) {
            case "default" -> Optional.of(SizeSlugMode.default_);
            case "thicknessdash" -> Optional.of(SizeSlugMode.thicknessDash);
            default -> Optional.empty();
        };
    }

    private static SizeKey parseSizeSafe(String s) {
        try {
            return SizeKey.parse(s);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Bad size in sizes.yml: '" + s + "'", ex);
        }
    }

    // ==== YAML DTO (POJO) ====

    /** Корневой DTO для SnakeYAML. */
    @SuppressWarnings("unused")
    private static final class YamlRoot {
        public Integer version;
        public String baselineCompetitor;
        public Map<String, YamlCompetitor> competitors;
        public Map<String, YamlCategory> categories;
    }

    @SuppressWarnings("unused")
    private static final class YamlCompetitor {
        public Boolean enabled;
        public String baseUrl;
        public YamlUrl url;
    }

    @SuppressWarnings("unused")
    private static final class YamlUrl {
        public String sizeSlug; // "default" | "thicknessDash"
    }

    @SuppressWarnings("unused")
    private static final class YamlCategory {
        public List<String> all;
        public Map<String, YamlOverrides> byCompetitor;
    }

    @SuppressWarnings("unused")
    private static final class YamlOverrides {
        public List<String> include;
        public List<String> exclude;
    }
}
