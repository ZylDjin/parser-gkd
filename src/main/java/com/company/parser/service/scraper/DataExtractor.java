package com.company.parser.service.scraper;

import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Site;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DataExtractor {

    /**
     * Извлечение данных из документа
     */
    public List<PriceRecord> extractData(Document document, Site site) {
        // Базовая реализация - возвращаем пустой список
        return new ArrayList<>();
    }
}
