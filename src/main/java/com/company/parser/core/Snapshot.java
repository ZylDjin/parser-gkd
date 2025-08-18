package com.company.parser.core;

import java.math.BigDecimal;
import java.util.*;

/**
 * В памяти хранит цены по ключу (Category, SizeKey, Competitor).
 * Позволяет читать/писать значения, а также итерироваться по всем записям.
 */
public final class Snapshot {

    // Category -> SizeKey -> Competitor -> Price
    private final EnumMap<Category, Map<SizeKey, EnumMap<Competitor, BigDecimal>>> data =
            new EnumMap<>(Category.class);

    /** Получить цену из снапшота. Возвращает null, если записи нет. */
    public BigDecimal get(Category category, SizeKey size, Competitor competitor) {
        var byCat = data.get(category);
        if (byCat == null) return null;
        var bySize = byCat.get(size);
        if (bySize == null) return null;
        return bySize.get(competitor);
    }

    /** Записать/заменить цену для ключа. Null-цены не допускаются. */
    public void put(Category category, SizeKey size, Competitor competitor, BigDecimal price) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(competitor, "competitor");
        Objects.requireNonNull(price, "price");
        var byCat = data.computeIfAbsent(category, c -> new LinkedHashMap<>());
        var bySize = byCat.computeIfAbsent(size, s -> new EnumMap<>(Competitor.class));
        bySize.put(competitor, price);
    }

    /** Удалить запись, если она была. */
    public void remove(Category category, SizeKey size, Competitor competitor) {
        var byCat = data.get(category);
        if (byCat == null) return;
        var bySize = byCat.get(size);
        if (bySize == null) return;
        bySize.remove(competitor);
        if (bySize.isEmpty()) byCat.remove(size);
        if (byCat.isEmpty()) data.remove(category);
    }

    /** Есть ли хоть одна запись. */
    public boolean isEmpty() {
        if (data.isEmpty()) return true;
        for (var m : data.values()) if (!m.isEmpty()) return false;
        return true;
    }

    /** Все категории, присутствующие в снапшоте (в порядке вставки). */
    public Set<Category> categories() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /** Все размеры по категории (в порядке вставки). */
    public Set<SizeKey> sizes(Category category) {
        var byCat = data.get(category);
        if (byCat == null) return Set.of();
        return Collections.unmodifiableSet(byCat.keySet());
    }

    /** Все конкуренты с ценами по паре (category,size). */
    public Map<Competitor, BigDecimal> competitorsMap(Category category, SizeKey size) {
        var byCat = data.get(category);
        if (byCat == null) return Map.of();
        var bySize = byCat.get(size);
        if (bySize == null) return Map.of();
        return Collections.unmodifiableMap(bySize);
    }

    /** Итератор по всем строкам снапшота. Удобно для сохранения в CSV. */
    public Iterable<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        for (var ce : data.entrySet()) {
            Category cat = ce.getKey();
            for (var se : ce.getValue().entrySet()) {
                SizeKey size = se.getKey();
                for (var ke : se.getValue().entrySet()) {
                    list.add(new Entry(cat, size, ke.getKey(), ke.getValue()));
                }
            }
        }
        return list;
    }

    /** Одна запись снапшота. */
    public record Entry(Category category, SizeKey size, Competitor competitor, BigDecimal price) {}
}
