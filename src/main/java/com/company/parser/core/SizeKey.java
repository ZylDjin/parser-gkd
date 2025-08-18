package com.company.parser.core;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Ключ размера: A x B x T.
 * Поддерживает парсинг строк вида "40x20x1.5", "40×20×1,5", "40 х 20 х 1.5 мм" и т.п.
 * Нормализует десятичные разделители, допускает кириллическую 'х' и знак '×'.
 */
public final class SizeKey implements Comparable<SizeKey> {

    private final BigDecimal a; // первая сторона
    private final BigDecimal b; // вторая сторона
    private final BigDecimal t; // толщина

    public SizeKey(BigDecimal a, BigDecimal b, BigDecimal t) {
        this.a = normalize(a);
        this.b = normalize(b);
        this.t = normalize(t);
        validate();
    }

    /** Создать из чисел. */
    public static SizeKey of(Number a, Number b, Number t) {
        return new SizeKey(new BigDecimal(a.toString()),
                new BigDecimal(b.toString()),
                new BigDecimal(t.toString()));
    }

    /** Разобрать из строки (поддерживаются x, X, ×, * и кириллическая х/Х; запятая = точка; допускается "мм"). */
    public static SizeKey parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("Size string is null");
        String s = raw.trim()
                .replace('\u00A0', ' ')                     // неразрывный пробел
                .replace('×', 'x')                          // знак умножения
                .replace('X', 'x')
                .replace('Х', 'x')                          // кириллическая Х
                .replace('х', 'x')                          // кириллическая х
                .replace('*', 'x')
                .replace("мм", "")
                .toLowerCase(Locale.ROOT);

        // уберём лишние пробелы вокруг 'x'
        s = s.replaceAll("\\s*x\\s*", "x");
        // десятичная запятая -> точка
        s = s.replace(',', '.');

        String[] parts = s.split("x");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Bad size format: " + raw);
        }

        try {
            BigDecimal a = new BigDecimal(parts[0].trim());
            BigDecimal b = new BigDecimal(parts[1].trim());
            BigDecimal t = new BigDecimal(parts[2].trim());
            return new SizeKey(a, b, t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad size: " + raw, e);
        }
    }

    /** "40x20x1.5" — каноничная форма без лишних нулей. */
    @Override
    public String toString() {
        return fmt(a) + "x" + fmt(b) + "x" + fmt(t);
    }

    /** Слаг для URL вида "40x20x1-5" (заменяем только дробную точку у толщины на дефис, как у AGRUPP). */
    public String slugThicknessDash() {
        String tt = fmt(t).replace('.', '-');
        return fmt(a) + "x" + fmt(b) + "x" + tt;
    }

    public BigDecimal a() { return a; }
    public BigDecimal b() { return b; }
    public BigDecimal t() { return t; }

    private static BigDecimal normalize(BigDecimal v) {
        if (v == null) return null;
        // убираем хвостовые нули и нормализуем масштаб
        return v.stripTrailingZeros();
    }

    private static String fmt(BigDecimal v) {
        String p = v.stripTrailingZeros().toPlainString();
        // "1." -> "1"
        if (p.endsWith(".")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private void validate() {
        if (a == null || b == null || t == null)
            throw new IllegalArgumentException("Size parts must not be null");
        if (a.signum() <= 0 || b.signum() <= 0 || t.signum() <= 0)
            throw new IllegalArgumentException("Size must be positive: " + toString());
    }

    // equals/hashCode — по числовому значению (без учёта масштаба BigDecimal)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeKey that)) return false;
        return a.compareTo(that.a) == 0
                && b.compareTo(that.b) == 0
                && t.compareTo(that.t) == 0;
    }

    @Override
    public int hashCode() {
        // хэш по каноничной строке, чтобы игнорировать scale
        return Objects.hash(fmt(a), fmt(b), fmt(t));
    }

    /** Сортировка по A, затем B, затем T (по возрастанию). */
    @Override
    public int compareTo(SizeKey o) {
        int c = this.a.compareTo(o.a);
        if (c != 0) return c;
        c = this.b.compareTo(o.b);
        if (c != 0) return c;
        return this.t.compareTo(o.t);
    }
}
