package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Утилиты валидации
 */
@Component
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    /**
     * Проверка валидности URL
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Проверка валидности email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Проверка валидности телефона
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        String cleaned = phone.replaceAll("[^0-9+]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Проверка на непустую строку
     */
    public boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Проверка диапазона чисел
     */
    public boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Проверка диапазона для double
     */
    public boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
