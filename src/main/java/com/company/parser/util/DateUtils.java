package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Утилиты для работы с датами
 */
@Component
public class DateUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Форматирование даты
     */
    public String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT);
    }

    /**
     * Форматирование даты и времени
     */
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMAT);
    }

    /**
     * Форматирование Instant
     */
    public String formatInstant(Instant instant) {
        if (instant == null) return "";
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT);
    }

    /**
     * Парсинг даты
     */
    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Получение начала дня
     */
    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Получение конца дня
     */
    public Instant endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Расчет разницы в днях
     */
    public long daysBetween(Instant start, Instant end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Человекочитаемый формат длительности
     */
    public String formatDuration(Duration duration) {
        if (duration == null) return "";

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%d ч %d мин %d сек", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d мин %d сек", minutes, seconds);
        } else {
            return String.format("%d сек", seconds);
        }
    }

    /**
     * Проверка, что дата в прошлом
     */
    public boolean isPast(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }

    /**
     * Проверка, что дата в будущем
     */
    public boolean isFuture(Instant instant) {
        return instant != null && instant.isAfter(Instant.now());
    }
}