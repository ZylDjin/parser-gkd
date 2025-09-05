package com.company.parser.client;

import com.company.parser.config.AppProperties;
import com.company.parser.exception.HttpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP клиент для выполнения запросов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClient {

    private final java.net.http.HttpClient client;
    private final AppProperties appProperties;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final RetryHandler retryHandler;

    /**
     * GET запрос с получением HTML документа
     */
    @Cacheable(value = "httpResponses", key = "#url")
    public Document get(String url) throws IOException {
        return get(url, Map.of());
    }

    /**
     * GET запрос с заголовками
     */
    public Document get(String url, Map<String, String> headers) throws IOException {
        String html = getString(url, headers);
        return Jsoup.parse(html, url);
    }

    /**
     * GET запрос с получением строки
     */
    public String getString(String url) throws IOException {
        return getString(url, Map.of());
    }

    /**
     * GET запрос с заголовками и получением строки
     */
    public String getString(String url, Map<String, String> headers) throws IOException {
        // Проверка Circuit Breaker
        String domain = extractDomain(url);
        if (!circuitBreaker.allowRequest(domain)) {
            throw new HttpException(url, 0, "Circuit breaker is open for domain: " + domain);
        }

        try {
            // Rate limiting
            rateLimiter.acquire();

            // Выполнение запроса с повторными попытками
            String response = retryHandler.executeWithRetry(() ->
                    executeRequest(url, headers)
            );

            circuitBreaker.recordSuccess(domain);
            return response;

        } catch (Exception e) {
            circuitBreaker.recordFailure(domain);

            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("HTTP request failed", e);
        }
    }

    /**
     * Асинхронный GET запрос
     */
    public CompletableFuture<Document> getAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * POST запрос
     */
    public String post(String url, String body, Map<String, String> headers) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(appProperties.http().timeoutSec()))
                .header("User-Agent", appProperties.http().userAgent())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        headers.forEach(requestBuilder::header);

        try {
            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw new HttpException(url, response.statusCode(),
                    "POST request failed with status: " + response.statusCode());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Выполнение HTTP запроса
     */
    private String executeRequest(String url, Map<String, String> headers) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(appProperties.http().timeoutSec()))
                .header("User-Agent", appProperties.http().userAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .GET();

        // Добавление пользовательских заголовков
        headers.forEach(requestBuilder::header);

        // Добавление заголовков по умолчанию из конфигурации
        if (appProperties.http().defaultHeaders() != null) {
            appProperties.http().defaultHeaders().forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        try {
            log.debug("HTTP GET: {}", url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            log.debug("HTTP Response: {} - Status: {}", url, statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                return response.body();
            } else if (statusCode == 404) {
                throw new HttpException(url, statusCode, "Page not found");
            } else if (statusCode == 403) {
                throw new HttpException(url, statusCode, "Access forbidden");
            } else if (statusCode == 429) {
                throw new HttpException(url, statusCode, "Rate limit exceeded");
            } else if (statusCode >= 500) {
                throw new HttpException(url, statusCode, "Server error");
            } else {
                throw new HttpException(url, statusCode, "HTTP error");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Извлечение домена из URL
     */
    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
