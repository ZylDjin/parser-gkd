package com.company.parser.core;


import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class DevRunner {
    public static void main(String[] args) throws Exception {
        // 0) Загружаем sizes.conf: сначала внешний файл, иначе ресурс из JAR
        Path external = Path.of("config", "sizes.conf");
        SizesConfig sizesCfg = Files.exists(external)
                ? SizesConfig.load(external)
                : SizesConfig.loadFromResource("config/sizes.conf");

        // 1) Категория отчёта (по ТЗ сейчас СП — профильные трубы)
        Category category = Category.SP;

        // 2) Список размеров = Юнион по всем компаниям в этой категории
        List<SizeKey> sizes = sizesCfg.sizesUnion(category);
        if (sizes.isEmpty()) {
            System.out.println("Нет размеров в категории " + category + " в sizes.conf");
            return;
        }

        // 3) Регистрация доступных Парсеров по компаниям
        Map<Competitor, SiteParser> allParsers = new EnumMap<>(Competitor.class);
        allParsers.put(Competitor.AGRUPP, new AgruppPlaywrightParser(
                "https://ag.market/catalog/truby-stalnye/truby-profilnye/"
        ));
        // Временная заглушка для Металлоторга (позже заменим на реальный Парсер)
        allParsers.put(Competitor.METALLOTORG, new FakeParser(Competitor.METALLOTORG));

        // Компании, у которых есть размеры в этой категории (по конфигу)
        Set<Competitor> activeComps = sizesCfg.competitorsForCategory(category);

        PriceSelectorService selector = new PriceSelectorService();

        // 4) prev/cur снапшоты (и защита от мусорных значений)
        Snapshot prev = new SnapshotStore(Path.of("snapshot_prev.csv")).load();
        prev.data().entrySet().removeIf(e ->
                e.getValue() != null &&
                        e.getValue().toPlainString().replaceAll("\\D", "").length() > 7
        );
        Snapshot cur = new Snapshot();

        // 5) Наша цена (можно вынести в файл аналогично sizes.conf)
        Map<SizeKey, BigDecimal> our = Map.of(
                SizeKey.parse("40x20x1.5"), new BigDecimal("50000"),
                SizeKey.parse("25x25x1.5"), new BigDecimal("47000")
        );

        ReportCalculator calc = new ReportCalculator();

        System.out.println("SIZE\tOUR\t\tCOMP(old|new|diff)...\t\tmin(new)\tavg(new)\tΔ(min-our)");

        int foundNewCount = 0;

        for (SizeKey s : sizes) {
            // 6) На каждый размер берём только те парсеры, чьи компании действительно мониторят этот размер
            List<SiteParser> parsersForSize = new ArrayList<>();
            for (Competitor c : activeComps) {
                if (!sizesCfg.hasSize(c, category, s)) continue;
                SiteParser sp = allParsers.get(c);
                if (sp != null) parsersForSize.add(sp);
            }

            // Если парсеров для этого размера нет — всё равно считаем строку (покажет old из prev)
            var row = calc.calcRow(category, s, parsersForSize, selector, prev, our);

            // накапливаем «new» в текущий снимок
            for (var e : row.newPrice.entrySet()) {
                if (e.getValue() != null) {
                    cur.put(category, s, e.getKey(), e.getValue());
                    foundNewCount++;
                }
            }

            // консольный отчёт
            System.out.printf("%s\t%s\t", s, our.get(s) == null ? "-" : our.get(s));
            for (var comp : Competitor.values()) {
                var o = row.oldPrice.get(comp);
                var n = row.newPrice.get(comp);
                var d = row.diff.get(comp);
                System.out.printf("%s(%s|%s|%s)\t",
                        comp.name(),
                        o == null ? "-" : o.toPlainString(),
                        n == null ? "-" : n.toPlainString(),
                        d == null ? "-" : d.toPlainString());
            }
            System.out.printf("\t%s\t%s\t%s%n",
                    row.minNew == null ? "-" : row.minNew.toPlainString(),
                    row.avgNew == null ? "-" : row.avgNew.toPlainString(),
                    row.deltaFromOur == null ? "-" : row.deltaFromOur.toPlainString());
        }

        // 7) сохраняем текущий снимок и при успехе двигаем baseline
        new SnapshotStore(Path.of("snapshot_cur.csv")).save(cur);
        System.out.println("\nSaved snapshot_cur.csv");

        if (foundNewCount > 0) {
            Files.copy(Path.of("snapshot_cur.csv"), Path.of("snapshot_prev.csv"), REPLACE_EXISTING);
            System.out.println("Baseline advanced: snapshot_prev.csv updated.");
        } else {
            System.out.println("Baseline NOT advanced (no new prices found).");
        }

        System.out.println("Done.");
    }
}
