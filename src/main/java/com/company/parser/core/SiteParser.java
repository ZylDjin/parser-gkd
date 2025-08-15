package com.company.parser.core;

import java.util.List;

public interface SiteParser {
    Competitor competitor();
    List<PriceVariant> fetch(Category category, SizeKey size) throws Exception;
}
