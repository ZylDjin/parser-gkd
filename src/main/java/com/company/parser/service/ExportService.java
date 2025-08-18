package com.company.parser.service;

import com.company.parser.core.Category;
import com.company.parser.core.ReportCalculator;
import com.company.parser.export.ExcelExporter;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class ExportService {
    private final ExcelExporter exporter;

    public ExportService(ExcelExporter exporter) {
        this.exporter = exporter;
    }

    public Path exportMatrix(Category category, List<ReportCalculator.Row> rows, String outPath) throws Exception {
        return exporter.export(category, rows, Path.of(outPath));
    }
}
