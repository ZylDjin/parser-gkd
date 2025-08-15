package com.company.parser.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Snapshot {
    private final Map<String, BigDecimal> map = new HashMap<>();

    public static String key(Category c, SizeKey s, Competitor comp) {
        return c + "|" + s + "|" + comp.name();
    }
    public void put(Category c, SizeKey s, Competitor comp, BigDecimal price) {
        map.put(key(c, s, comp), price);
    }
    public BigDecimal get(Category c, SizeKey s, Competitor comp) {
        return map.get(key(c, s, comp));
    }
    public Map<String, BigDecimal> data() { return map; }
}
