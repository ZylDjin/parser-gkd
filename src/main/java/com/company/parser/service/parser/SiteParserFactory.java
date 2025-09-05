package com.company.parser.service.parser;

import com.company.parser.exception.ParserException;
import com.company.parser.model.enums.ParserType;
import com.company.parser.parser.base.SiteParser;
import com.company.parser.parser.impl.AgMarketParser;
import com.company.parser.parser.impl.DemidovParser;
import com.company.parser.parser.impl.GenericCssParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика для создания парсеров
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteParserFactory {

    private final GenericCssParser genericCssParser;
    private final DemidovParser demidovParser;
    private final AgMarketParser agMarketParser;

    private final Map<String, SiteParser> customParsers = new HashMap<>();

    /**
     * Получение парсера по типу
     */
    public SiteParser getParser(String parserType) {
        if (parserType == null) {
            return genericCssParser;
        }

        ParserType type;
        try {
            type = ParserType.fromName(parserType);
        } catch (Exception e) {
            log.warn("Unknown parser type: {}, using GENERIC", parserType);
            return genericCssParser;
        }

        return switch (type) {
            case GENERIC -> genericCssParser;
            case CUSTOM -> getCustomParser(parserType);
            case API -> throw new ParserException("API parser not implemented");
            case JAVASCRIPT -> throw new ParserException("JavaScript parser not implemented");
            case XML -> throw new ParserException("XML parser not implemented");
        };
    }

    /**
     * Получение парсера для конкретного сайта
     */
    public SiteParser getParserForSite(String siteId) {
        return switch (siteId.toLowerCase()) {
            case "demidov" -> demidovParser;
            case "agmarket" -> agMarketParser;
            default -> genericCssParser;
        };
    }

    /**
     * Регистрация кастомного парсера
     */
    public void registerCustomParser(String siteId, SiteParser parser) {
        customParsers.put(siteId, parser);
        log.info("Registered custom parser for site: {}", siteId);
    }

    private SiteParser getCustomParser(String parserType) {
        SiteParser parser = customParsers.get(parserType);
        if (parser != null) {
            return parser;
        }

        // Fallback на generic
        log.warn("Custom parser not found for type: {}, using GENERIC", parserType);
        return genericCssParser;
    }
}
