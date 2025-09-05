package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилиты для работы с файлами
 */
@Component
public class FileUtils {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /**
     * Создание директории если не существует
     */
    public void ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Генерация имени файла с временной меткой
     */
    public String generateFileName(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return String.format("%s_%s.%s", prefix, timestamp, extension);
    }

    /**
     * Получение расширения файла
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Проверка существования файла
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Удаление файла
     */
    public boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Получение размера файла в человекочитаемом формате
     */
    public String getFileSize(String filePath) {
        try {
            long bytes = Files.size(Paths.get(filePath));
            return formatBytes(bytes);
        } catch (IOException e) {
            return "0 B";
        }
    }

    /**
     * Форматирование размера в байтах
     */
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
