package com.company.parser.service;

import com.company.parser.config.AppProperties;
import com.company.parser.config.SizesConfig;
import com.company.parser.core.*;
import com.company.parser.export.ExcelExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ExcelExporter exporter;
    private final SizesConfig sizesConfig;
    private final AppProperties props;

    public ExportService(ExcelExporter exporter, SizesConfig sizesConfig, AppProperties props) {
        this.exporter = exporter;
        this.sizesConfig = sizesConfig;
        this.props = props;
    }

    /** Экспорт «перевёрнутой» матрицы: размеры — колонки, компании — строки. */
    public Path exportTransposedDefault(Category category, Snapshot snapshot) throws Exception {
        List<SizeKey> sizes = sizesConfig.sizesUnion(category);

        // конкуренты из application.yml, отфильтрованные по включённости в sizes.yml
        List<Competitor> enabled = new ArrayList<>();
        for (Competitor c : props.getCompetitorsEnabled()) {
            if (sizesConfig.competitorEnabled(category, c)) enabled.add(c);
        }

        Path out = Path.of(props.getExportPath()).toAbsolutePath();
        log.info("[ExportService] exportTransposedDefault -> {}", out);
        exporter.exportMatrixTransposed(out, snapshot, category, sizes, enabled, sizesConfig);
        return out;
    }
}
