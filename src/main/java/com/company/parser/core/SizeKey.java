package com.company.parser.core;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ключ размера: ширина x высота x толщина.
 * Примеры валидных строк:
 *  - "40x20x1.5"
 *  - "40×20×1,5"
 *  - "40 x 20 x 1-5"
 *  - пробелы допускаются; разделитель — 'x'/'X'/'×'; толщина может быть с точкой, запятой или дефисом.
 */
public final class SizeKey implements Comparable<SizeKey> {

    private final int width;
    private final int height;
    private final BigDecimal thickness; // храним в виде десятичного числа (1.5, 2, 3.0 и т.п.)

    private static final Pattern PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s*[xX×]\\s*(\\d+)\\s*[xX×]\\s*([0-9]+(?:[.,-][0-9])?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public SizeKey(int width, int height, BigDecimal thickness) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width/height must be positive");
        }
        if (thickness == null || thickness.signum() <= 0) {
            throw new IllegalArgumentException("Thickness must be positive");
        }
        this.width = width;
        this.height = height;
        this.thickness = thickness.stripTrailingZeros();
    }

    /** Толерантный парсинг строки размера. Бросает IllegalArgumentException при ошибке. */
    public static SizeKey parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("Size is null");
        // частая ошибка — кириллическая «х». нормализуем на входе.
        String norm = raw.replace('Х', 'x').replace('х', 'x');
        Matcher m = PATTERN.matcher(norm);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad size: " + raw);
        }
        int w = Integer.parseInt(m.group(1));
        int h = Integer.parseInt(m.group(2));
        String tRaw = m.group(3).trim();

        // толщина может быть "1-5" / "1,5" / "1.5" / "2"
        String tDot = tRaw.replace('-', '.').replace(',', '.');
        BigDecimal t;
        try {
            t = new BigDecimal(tDot);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Bad thickness in size: " + raw);
        }
        return new SizeKey(w, h, t);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public BigDecimal thickness() {
        return thickness;
    }

    /** Толщина в точечном формате (1.5 / 2 / 3.8). */
    public String thicknessDot() {
        return thickness.stripTrailingZeros().toPlainString();
    }

    /** Толщина в дефисном формате (1-5 / 2 / 3-8) — нужна для slug у некоторых сайтов (AGRUPP). */
    public String thicknessDash() {
        String s = thicknessDot();
        // только десятичную часть переводим в дефис (1.5 -> 1-5), целое остаётся без дефиса (2 -> 2)
        int idx = s.indexOf('.');
        if (idx < 0) return s;
        return s.substring(0, idx) + "-" + s.substring(idx + 1);
    }

    /** Строковое представление стандартизовано как W×H×T с точкой в толщине. */
    @Override
    public String toString() {
        return width + "x" + height + "x" + thicknessDot();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeKey sizeKey)) return false;
        return width == sizeKey.width &&
                height == sizeKey.height &&
                thickness.compareTo(sizeKey.thickness) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, thickness.stripTrailingZeros());
    }

    @Override
    public int compareTo(SizeKey o) {
        int c = Integer.compare(this.width, o.width);
        if (c != 0) return c;
        c = Integer.compare(this.height, o.height);
        if (c != 0) return c;
        return this.thickness.compareTo(o.thickness);
    }
}
