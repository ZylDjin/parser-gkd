package com.company.parser.export;


import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.SizeKey;
import com.company.parser.config.SizesConfig;
import com.company.parser.core.Snapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Экспорт отчёта в XLSX.
 * "Перевёрнутая" матрица: размеры — СТОЛБЦЫ, компании — СТРОКИ.
 * Структура листа MatrixT:
 *   A: Компания
 *   B: Показатель (new, Δ(new-<baseline>))
 *   C..: по одному столбцу на каждый размер (в порядке sizesOrdered).
 */
@Component
public class ExcelExporter {

    private static final Logger log = LoggerFactory.getLogger(ExcelExporter.class);

    public void exportMatrixTransposed(Path out,
                                       Snapshot snapshot,
                                       Category category,
                                       List<SizeKey> sizesOrdered,
                                       List<Competitor> enabledOrdered,
                                       SizesConfig sizesConfig) throws IOException {

        Competitor baseline = sizesConfig.getBaselineCompetitor();
        log.info("[ExcelExporter] Writing TRANSPOSED matrix -> {}, category={}, baseline={}, sizes={}, competitors={}",
                out.toAbsolutePath(), category, baseline, sizesOrdered.size(), enabledOrdered);

        try (Workbook wb = new XSSFWorkbook()) {
            // новый лист с уникальным именем, чтобы было очевидно, что ты смотришь новый отчёт
            Sheet sh = wb.createSheet("MatrixT");

            // ===== стили =====
            CreationHelper ch = wb.getCreationHelper();

            CellStyle head = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            head.setFont(headFont);
            head.setAlignment(HorizontalAlignment.CENTER);

            CellStyle text = wb.createCellStyle();

            CellStyle num = wb.createCellStyle();
            num.setDataFormat(ch.createDataFormat().getFormat("#,##0"));

            CellStyle diffPos = wb.createCellStyle();
            diffPos.cloneStyleFrom(num);
            diffPos.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            diffPos.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle diffNeg = wb.createCellStyle();
            diffNeg.cloneStyleFrom(num);
            diffNeg.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            diffNeg.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ===== заголовок =====
            Row r0 = sh.createRow(0);
            int c = 0;
            set(r0, c++, "Компания", head);
            set(r0, c++, "Показатель", head);
            for (SizeKey s : sizesOrdered) {
                set(r0, c++, s.toString(), head);
            }

            // порядок: baseline первым, затем остальные
            List<Competitor> ordered = new ArrayList<>();
            if (enabledOrdered.contains(baseline)) ordered.add(baseline);
            for (Competitor comp : enabledOrdered) if (comp != baseline) ordered.add(comp);

            // ===== строки =====
            int rowIdx = 1;
            for (Competitor comp : ordered) {
                // Строка "new"
                Row newRow = sh.createRow(rowIdx++);
                int cc = 0;
                set(newRow, cc++, comp.title(), text);
                set(newRow, cc++, "new", head);
                for (SizeKey size : sizesOrdered) {
                    BigDecimal price = snapshot.get(category, size, comp);
                    set(newRow, cc++, price, num);
                }

                // Строка "Δ(new-baseline)" — у baseline дельта пустая
                Row dRow = sh.createRow(rowIdx++);
                cc = 0;
                set(dRow, cc++, "", text);
                set(dRow, cc++, "Δ(new-" + baseline.title() + ")", head);
                for (SizeKey size : sizesOrdered) {
                    BigDecimal p = snapshot.get(category, size, comp);
                    BigDecimal base = snapshot.get(category, size, baseline);
                    if (comp == baseline || p == null || base == null) {
                        set(dRow, cc++, (String) null, text);
                    } else {
                        BigDecimal d = p.subtract(base);
                        set(dRow, cc++, d, d.signum() >= 0 ? diffPos : diffNeg);
                    }
                }
            }

            // автоширина
            for (int i = 0; i < c; i++) sh.autoSizeColumn(i);

            // запись файла
            if (out.getParent() != null) Files.createDirectories(out.getParent());
            try (var os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }

        log.info("[ExcelExporter] DONE: {}", out.toAbsolutePath());
    }

    // ==== helpers ====

    private static void set(Row r, int col, String v, CellStyle st) {
        Cell cell = r.createCell(col);
        if (v != null) cell.setCellValue(v);
        if (st != null) cell.setCellStyle(st);
    }

    private static void set(Row r, int col, BigDecimal v, CellStyle st) {
        Cell cell = r.createCell(col);
        if (v != null) cell.setCellValue(v.doubleValue());
        if (st != null) cell.setCellStyle(st);
    }
}
