package com.company.parser.export;

import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.ReportCalculator;
import com.company.parser.core.SizeKey;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExcelExporter {

    public Path export(Category category,
                       List<ReportCalculator.Row> rows,
                       Path outFile) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            buildMatrixSheet(wb, st, category, rows);

            try (FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
                wb.write(fos);
            }
        }
        return outFile;
    }

    private void buildMatrixSheet(Workbook wb, Styles st, Category category, List<ReportCalculator.Row> rows) {
        final Competitor BASELINE = Competitor.DEMIDOV;
        Sheet sh = wb.createSheet("Matrix");

        Row info = sh.createRow(0);
        set(info, 0, "Сформировано: " + now(), st.muted);
        set(info, 1, "Категория: " + category, st.muted);

        // размеры -> столбцы
        List<SizeKey> sizes = new ArrayList<>();
        Map<String, ReportCalculator.Row> bySize = new LinkedHashMap<>();
        for (ReportCalculator.Row r : rows) {
            sizes.add(r.size);
            bySize.put(r.size.toString(), r);
        }
        sizes.sort(Comparator.comparing(SizeKey::toString));

        int r0 = 2;
        Row h = sh.createRow(r0);
        int col = 0;
        set(h, col++, "Компания", st.head);
        set(h, col++, "Метрика", st.head);
        for (SizeKey s : sizes) set(h, col++, s.toString(), st.head);

        // baseline по размерам
        Map<SizeKey, BigDecimal> baseNew = new HashMap<>();
        for (SizeKey s : sizes) {
            ReportCalculator.Row rr = bySize.get(s.toString());
            baseNew.put(s, rr == null ? null : rr.newPrice.get(BASELINE));
        }

        int r = r0 + 1;

        // baseline first
        Row newRow = sh.createRow(r++);
        set(newRow, 0, title(BASELINE), st.text);
        set(newRow, 1, "new", st.text);
        int c = 2;
        for (SizeKey s : sizes) set(newRow, c++, baseNew.get(s), st.num);

        // остальные
        for (Competitor comp : Competitor.values()) {
            if (comp == BASELINE) continue;

            Row rowNew = sh.createRow(r++);
            set(rowNew, 0, title(comp), st.text);
            set(rowNew, 1, "new", st.text);
            c = 2;
            for (SizeKey s : sizes) {
                ReportCalculator.Row rr = bySize.get(s.toString());
                set(rowNew, c++, rr == null ? null : rr.newPrice.get(comp), st.num);
            }

            Row rowDelta = sh.createRow(r++);
            set(rowDelta, 0, title(comp), st.text);
            set(rowDelta, 1, "Δ(new−" + title(BASELINE) + ")", st.text);
            c = 2;
            for (SizeKey s : sizes) {
                ReportCalculator.Row rr = bySize.get(s.toString());
                BigDecimal vNew = (rr == null) ? null : rr.newPrice.get(comp);
                BigDecimal vBase = baseNew.get(s);
                BigDecimal delta = (vNew != null && vBase != null) ? vNew.subtract(vBase) : null;
                set(rowDelta, c++, delta, st.num);
            }
        }

        // агрегаты
        Row minRow = sh.createRow(r++);
        set(minRow, 0, "", st.text);
        set(minRow, 1, "min(new)", st.head);
        c = 2;
        for (SizeKey s : sizes) {
            ReportCalculator.Row rr = bySize.get(s.toString());
            set(minRow, c++, rr == null ? null : rr.minNew, st.num);
        }

        Row avgRow = sh.createRow(r++);
        set(avgRow, 0, "", st.text);
        set(avgRow, 1, "avg(new)", st.head);
        c = 2;
        for (SizeKey s : sizes) {
            ReportCalculator.Row rr = bySize.get(s.toString());
            set(avgRow, c++, rr == null ? null : rr.avgNew, st.num);
        }

        int lastRow = sh.getLastRowNum();
        int lastCol = sh.getRow(r0).getLastCellNum() - 1;

        for (int i = 0; i <= lastCol; i++) sh.autoSizeColumn(i);
        sh.createFreezePane(2, r0 + 1);

        var region = new CellRangeAddress(r0, lastRow, 0, lastCol);
        borderRegion(sh, region);

        // подсветка для Δ
        SheetConditionalFormatting scf = sh.getSheetConditionalFormatting();
        var red = scf.createConditionalFormattingRule(ComparisonOperator.GT, "0");
        var redFill = red.createPatternFormatting();
        redFill.setFillBackgroundColor(IndexedColors.ROSE.index);
        redFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        var green = scf.createConditionalFormattingRule(ComparisonOperator.LE, "0");
        var greenFill = green.createPatternFormatting();
        greenFill.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
        greenFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        for (int ri = r0 + 1; ri <= lastRow; ri++) {
            Row row = sh.getRow(ri);
            if (row == null) continue;
            Cell metric = row.getCell(1);
            if (metric != null && metric.getCellType() == CellType.STRING) {
                String label = metric.getStringCellValue();
                if (label != null && label.startsWith("Δ(")) {
                    scf.addConditionalFormatting(new CellRangeAddress[]{ new CellRangeAddress(ri, ri, 2, lastCol) }, red, green);
                }
            }
        }
    }

    private static String title(Competitor c) {
        return switch (c) {
            case DEMIDOV -> "ГК Демидов";
            default -> c.name();
        };
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static void borderRegion(Sheet sh, CellRangeAddress region) {
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sh);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sh);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sh);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sh);
    }

    private static void set(Row row, int col, String val, CellStyle st) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
        if (st != null) cell.setCellStyle(st);
    }
    private static void set(Row row, int col, BigDecimal val, CellStyle st) {
        Cell cell = row.createCell(col);
        if (val != null) cell.setCellValue(val.longValue());
        if (st != null) cell.setCellStyle(st);
    }

    private static final class Styles {
        final CellStyle head, text, num, muted;
        Styles(Workbook wb) {
            DataFormat fmt = wb.createDataFormat();

            head = wb.createCellStyle();
            var fb = wb.createFont(); fb.setBold(true);
            head.setFont(fb);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setAlignment(HorizontalAlignment.CENTER);
            head.setVerticalAlignment(VerticalAlignment.CENTER);
            borders(head);

            text = wb.createCellStyle();
            text.setVerticalAlignment(VerticalAlignment.CENTER);
            borders(text);

            num = wb.createCellStyle();
            num.cloneStyleFrom(text);
            num.setDataFormat(fmt.getFormat("#,##0"));

            muted = wb.createCellStyle();
            var fm = wb.createFont(); fm.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            muted.setFont(fm);
        }
        private void borders(CellStyle s) {
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }
}
