package com.company.parser.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FakeParser implements SiteParser {
    private final Competitor comp;
    public FakeParser(Competitor c){ this.comp = c; }

    @Override public Competitor competitor() { return comp; }

    @Override
    public List<PriceVariant> fetch(Category category, SizeKey size) {
        // Заглушка: эмулируем, что для некоторых размеров есть ГОСТ/ТУ
        List<PriceVariant> out = new ArrayList<>();
        if (category == Category.SP && size.toString().equals("40x20x1.5")) {
            out.add(new PriceVariant(new BigDecimal("52000"), true, false, "ГОСТ"));
            out.add(new PriceVariant(new BigDecimal("54000"), true, false, "ГОСТ"));
        } else if (category == Category.CHANNEL && size.toString().equals("80x32x4")) {
            out.add(new PriceVariant(new BigDecimal("56000"), false, true, "ТУ"));
            out.add(new PriceVariant(new BigDecimal("58000"), false, true, "ТУ"));
        }
        return out;
    }
}