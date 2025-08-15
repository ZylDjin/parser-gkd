package com.company.parser.core;

import java.math.BigDecimal;
import java.util.Locale;

public record SizeKey(BigDecimal a, BigDecimal b, BigDecimal t) {

    public static SizeKey parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("Size is null");

        // Нормализация: пробелы, разные варианты "x", запятая в толщине, NBSP
        String s = raw.trim()
                .replace('\u00A0', ' ')   // NBSP -> space
                .replace(" ", "")         // убрать все пробелы
                .replace('×', 'x')        // знак умножения -> x
                .replace('X', 'x')        // латинская заглавная X -> x
                .replace('х', 'x')        // КИРИЛЛИЧЕСКАЯ х -> x
                .replace(',', '.')        // запятая -> точка
                .toLowerCase(Locale.ROOT);

        String[] parts = s.split("x");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Bad size: " + raw);
        }

        try {
            BigDecimal a = new BigDecimal(parts[0]);
            BigDecimal b = new BigDecimal(parts[1]);
            BigDecimal t = new BigDecimal(parts[2]);
            return new SizeKey(a, b, t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad size: " + raw, e);
        }
    }

    @Override public String toString() {
        // Нормальная каноническая форма: A x B x T с точкой
        return a.stripTrailingZeros().toPlainString()
                + "x" + b.stripTrailingZeros().toPlainString()
                + "x" + t.stripTrailingZeros().toPlainString();
    }
}
