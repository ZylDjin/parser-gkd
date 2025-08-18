package com.company.parser.config;

import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.SizeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Конфиг размеров из sizes.yaml.
 *
 * Ожидаемая структура YAML (пример):
 *
 * SP:
 *   baseline: DEMIDOV
 *   sizes: ["25x25x1.5","40x20x1.5", ...]
 *   competitors:
 *     DEMIDOV:
 *       baseUrl: "https://demidovsteel.ru/catalog/truby-profilnye/"
 *     AGRUPP:
 *       baseUrl: "https://ag.market/catalog/truby-stalnye/truby-profilnye/"
 *       sizes: ["25x25x1.5","40x20x1.5"]  # (необязательно) override
 */
public final class SizesConfig {

    private static final Logger log = LoggerFactory.getLogger(SizesConfig.class);

    /** по категории — настройки */
    private final Map<Category, CategoryCfg> byCategory;
    /** дефолтный baseline, если в категории не задан */
    private final Competitor defaultBaseline;

    public SizesConfig(Map<Category, CategoryCfg> byCategory, Competitor defaultBaseline) {
        this.byCategory = new EnumMap<>(byCategory);
        this.defaultBaseline = defaultBaseline != null ? defaultBaseline : Competitor.DEMIDOV;
    }

    /** baseline для категории; если не задан в YAML — берём defaultBaseline */
    public Competitor baseline(Category category) {
        CategoryCfg cfg = byCategory.get(category);
        if (cfg != null && cfg.baseline != null) return cfg.baseline;
        return defaultBaseline;
    }

    /** общий список размеров по категории */
    public List<SizeKey> sizes(Category category) {
        CategoryCfg cfg = byCategory.get(category);
        if (cfg == null) return List.of();
        return cfg.baseSizes;
    }

    /**
     * список размеров с учётом override у конкурента (если задан).
     * Возвращает null/empty, если override отсутствует — вызывающая сторона решает, что брать общий список.
     */
    public List<SizeKey> sizes(Category category, Competitor competitor) {
        CategoryCfg cfg = byCategory.get(category);
        if (cfg == null || cfg.compSizes == null) return null;
        List<SizeKey> list = cfg.compSizes.get(competitor);
        return (list == null || list.isEmpty()) ? null : list;
    }

    /** базовый URL конкурента в рамках категории (может быть null) */
    public String baseUrl(Category category, Competitor competitor) {
        CategoryCfg cfg = byCategory.get(category);
        if (cfg == null || cfg.compBaseUrl == null) return null;
        return cfg.compBaseUrl.get(competitor);
    }

    /** доступные категории */
    public Set<Category> categories() {
        return Collections.unmodifiableSet(byCategory.keySet());
    }

    // ---------- загрузка YAML ----------

    @SuppressWarnings("unchecked")
    public static SizesConfig loadFromYaml(Resource yamlResource, Competitor defaultBaseline) {
        Map<Category, CategoryCfg> map = new EnumMap<>(Category.class);

        try (InputStream is = yamlResource.getInputStream()) {
            Object rootObj = new Yaml().load(is);
            if (!(rootObj instanceof Map<?, ?> root)) {
                throw new IllegalStateException("sizes.yaml has unexpected structure (root is not a map)");
            }

            for (Map.Entry<?, ?> e : root.entrySet()) {
                String catKey = String.valueOf(e.getKey()).trim();
                Category category = Category.valueOf(catKey); // названия в YAML = enum имена
                Object node = e.getValue();
                if (!(node instanceof Map<?, ?> catMap)) {
                    log.warn("Category '{}' node is not a map, skip", catKey);
                    continue;
                }

                CategoryCfg cfg = new CategoryCfg();
                // baseline
                Object bl = catMap.get("baseline");
                if (bl != null) {
                    cfg.baseline = parseCompetitor(String.valueOf(bl));
                }

                // base sizes
                cfg.baseSizes = parseSizesList(catMap.get("sizes"));

                // competitors
                Object compsNode = catMap.get("competitors");
                if (compsNode instanceof Map<?, ?> compsMap) {
                    cfg.compBaseUrl = new EnumMap<>(Competitor.class);
                    cfg.compSizes = new EnumMap<>(Competitor.class);

                    for (Map.Entry<?, ?> ce : compsMap.entrySet()) {
                        String compKey = String.valueOf(ce.getKey()).trim();
                        Competitor comp = parseCompetitor(compKey);
                        Object compNode = ce.getValue();

                        if (compNode instanceof Map<?, ?> compMap) {
                            // baseUrl
                            Object url = compMap.get("baseUrl");
                            if (url != null) {
                                cfg.compBaseUrl.put(comp, String.valueOf(url).trim());
                            }
                            // sizes override
                            Object sizesNode = compMap.get("sizes");
                            List<SizeKey> override = parseSizesList(sizesNode);
                            if (!override.isEmpty()) {
                                cfg.compSizes.put(comp, override);
                            }
                        }
                    }
                }

                map.put(category, cfg);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load sizes.yaml: " + ex.getMessage(), ex);
        }

        log.info("[SizesConfig] loaded. categories={}, defaultBaseline={}", map.keySet(), defaultBaseline);
        return new SizesConfig(map, defaultBaseline);
    }

    // ---------- helpers ----------

    private static Competitor parseCompetitor(String raw) {
        String key = raw.trim().toUpperCase(Locale.ROOT);
        return Competitor.valueOf(key);
    }

    @SuppressWarnings("unchecked")
    private static List<SizeKey> parseSizesList(Object node) {
        if (node == null) return List.of();
        if (!(node instanceof List<?> list)) return List.of();

        List<SizeKey> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;
            String s = String.valueOf(o).trim();
            try {
                out.add(SizeKey.parse(s));
            } catch (IllegalArgumentException iae) {
                // логируем и пропускаем битый размер, чтобы не падать загрузкой
                LoggerFactory.getLogger(SizesConfig.class).warn("Bad size in YAML: '{}', skip", s);
            }
        }
        return Collections.unmodifiableList(out);
    }

    // ---------- внутренние структуры ----------

    public static final class CategoryCfg {
        Competitor baseline;                              // baseline конкурент для категории
        List<SizeKey> baseSizes = List.of();              // общий список размеров
        Map<Competitor, List<SizeKey>> compSizes;         // override размеров на конкурента
        Map<Competitor, String> compBaseUrl;              // override baseUrl на конкурента
    }
}
