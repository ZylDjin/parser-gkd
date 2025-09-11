package com.company.parser.service.scraper;

import com.company.parser.model.domain.Site;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PageProcessor {

    /**
     * Обработка HTML документа
     */
    public Document process(Document document, Site site) {
        // Базовая обработка - просто возвращаем документ
        return document;
    }
}
