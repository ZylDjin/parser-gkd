package com.company.parser.service;

import com.company.parser.config.AppProperties;
import com.company.parser.core.Competitor;
import com.company.parser.core.parsers.AgruppPlaywrightParser;
import com.company.parser.core.parsers.DemidovPlaywrightParser;
import com.company.parser.core.parsers.SiteParser;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class ParserRegistry {
    private final Map<Competitor, SiteParser> parsers = new EnumMap<>(Competitor.class);

    public ParserRegistry(AppProperties props,
                          AgruppPlaywrightParser agrupp,
                          DemidovPlaywrightParser demidov) {
        var base = props.getCompetitorsBaseUrls();
        if (base != null) {
            if (base.containsKey("AGRUPP")) agrupp.setBaseUrl(base.get("AGRUPP"));
            if (base.containsKey("DEMIDOV")) demidov.setBaseUrl(base.get("DEMIDOV"));
        }
        parsers.put(Competitor.AGRUPP, agrupp);
        parsers.put(Competitor.DEMIDOV, demidov);
        // остальные добавим по мере готовности
    }

    public SiteParser get(Competitor c) { return parsers.get(c); }
}
