package com.company.parser.core.parsers;

import com.company.parser.config.AppProperties;
import com.company.parser.core.Competitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Реестр парсеров: маппинг Competitor -> SiteParser.
 * Учитывает whitelist из application.yml (app.competitorsEnabled).
 */
@Component
public class ParserRegistry {

    private static final Logger log = LoggerFactory.getLogger(ParserRegistry.class);

    private final Map<Competitor, SiteParser> all = new EnumMap<>(Competitor.class);
    private final Set<Competitor> enabled;

    public ParserRegistry(List<SiteParser> parsers, AppProperties props) {
        for (SiteParser p : parsers) {
            var comp = p.competitor();
            var prev = all.put(comp, p);
            if (prev != null) {
                log.warn("Duplicate parser for {}: {} overridden by {}", comp, prev.getClass().getSimpleName(), p.getClass().getSimpleName());
            }
        }
        Set<Competitor> cfg = new HashSet<>(props.getCompetitorsEnabled());
        // если список пуст — включаем всех найденных
        if (cfg.isEmpty()) {
            cfg.addAll(all.keySet());
        }
        enabled = Collections.unmodifiableSet(cfg);
        log.info("[ParserRegistry] enabled: {}", enabled);
    }

    /** Получить парсер по конкуренту. */
    public Optional<SiteParser> get(Competitor competitor) {
        return Optional.ofNullable(all.get(competitor));
    }

    /** Список включённых конкурентов (по конфигу). */
    public Set<Competitor> enabledCompetitors() {
        return enabled;
    }

    /** Включён ли конкретный конкурент. */
    public boolean isEnabled(Competitor competitor) {
        return enabled.contains(competitor);
    }

    /** Все включённые парсеры. */
    public List<SiteParser> enabledParsers() {
        List<SiteParser> out = new ArrayList<>();
        for (var c : enabled) {
            var p = all.get(c);
            if (p != null) out.add(p);
        }
        return out;
    }
}