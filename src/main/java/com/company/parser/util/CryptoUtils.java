package com.company.parser.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Утилиты для криптографии и хэширования
 */
@Component
public class CryptoUtils {

    /**
     * Генерация MD5 хэша
     */
    public String md5(String input) {
        return hash(input, "MD5");
    }

    /**
     * Генерация SHA-256 хэша
     */
    public String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * Универсальный метод хэширования
     */
    private String hash(String input, String algorithm) {
        if (input == null) return null;

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : hashBytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not found: " + algorithm, e);
        }
    }

    /**
     * Base64 кодирование
     */
    public String base64Encode(String input) {
        if (input == null) return null;
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 декодирование
     */
    public String base64Decode(String encoded) {
        if (encoded == null) return null;
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Генерация уникального идентификатора
     */
    public String generateId(String... parts) {
        String combined = String.join("-", parts);
        return sha256(combined).substring(0, 16);
    }
}
