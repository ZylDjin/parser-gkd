package com.company.parser.core.parsers;

import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.PriceVariant;
import com.company.parser.core.SizeKey;

import java.util.List;

public interface SiteParser {
    Competitor competitor();
    List<PriceVariant> fetch(Category category, SizeKey size) throws Exception;
}
