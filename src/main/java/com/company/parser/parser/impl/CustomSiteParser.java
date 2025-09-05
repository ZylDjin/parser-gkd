package com.company.parser.parser.impl;

import com.company.parser.client.HttpClient;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import com.company.parser.parser.base.AbstractSiteParser;
import com.company.parser.parser.base.ParsingContext;
import com.company.parser.util.SizeNormalizer;
import com.company.parser.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Кастомный парсер для специфических сайтов
 * Базовый класс для создания специализированных парсеров
 */
@Slf4j
@Component("customSiteParser")
public abstract class CustomSiteParser extends AbstractSiteParser {

    @Autowired
    public CustomSiteParser(HttpClient httpClient, TextUtils textUtils, SizeNormalizer sizeNormalizer) {
        super(httpClient, textUtils, sizeNormalizer);
    }

    @Override
    public String getType() {
        return "CUSTOM";
    }

    @Override
    public boolean supports(Site site) {
        return "CUSTOM".equalsIgnoreCase(site.getParserType()) &&
                getSupportedSiteId().equalsIgnoreCase(site.getId());
    }

    /**
     * Получение ID поддерживаемого сайта
     */
    protected abstract String getSupportedSiteId();
}
