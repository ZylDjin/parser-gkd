package com.company.parser.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Конфигурация HTTP клиента
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final AppProperties appProperties;

    /**
     * Основной HTTP клиент
     */
    @Bean
    @Primary
    public HttpClient httpClient() {
        var httpConfig = appProperties.http();

        var builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(httpConfig.timeoutSec()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newVirtualThreadPerTaskExecutor());

        // Настройка прокси если включено
        if (httpConfig.proxy() != null && Boolean.TRUE.equals(httpConfig.proxy().enabled())) {
            builder.proxy(createProxySelector());
        }

        HttpClient client = builder.build();

        log.info("HTTP client configured: timeout={}s, maxRetries={}, userAgent={}",
                httpConfig.timeoutSec(),
                httpConfig.maxRetries(),
                httpConfig.userAgent());

        return client;
    }

    /**
     * HTTP клиент без прокси (для внутренних запросов)
     */
    @Bean(name = "internalHttpClient")
    public HttpClient internalHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    /**
     * HTTP клиент с коротким таймаутом (для health checks)
     */
    @Bean(name = "healthCheckHttpClient")
    public HttpClient healthCheckHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(Executors.newCachedThreadPool())
                .build();
    }

    /**
     * Создание ProxySelector
     */
    private ProxySelector createProxySelector() {
        var proxyConfig = appProperties.http().proxy();

        return new ProxySelector() {
            @Override
            public List<java.net.Proxy> select(URI uri) {
                // Проверка на non-proxy hosts
                if (proxyConfig.nonProxyHosts() != null) {
                    String host = uri.getHost();
                    for (String nonProxyHost : proxyConfig.nonProxyHosts()) {
                        if (host.matches(nonProxyHost.replace("*", ".*"))) {
                            return List.of(java.net.Proxy.NO_PROXY);
                        }
                    }
                }

                // Возвращаем прокси
                InetSocketAddress proxyAddress = new InetSocketAddress(
                        proxyConfig.host(),
                        proxyConfig.port()
                );

                return List.of(new java.net.Proxy(
                        java.net.Proxy.Type.HTTP,
                        proxyAddress
                ));
            }

            @Override
            public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                log.error("Failed to connect through proxy to {}: {}", uri, ioe.getMessage());
            }
        };
    }

    /**
     * Конфигурация для HTTP запросов
     */
    @Bean
    public HttpRequestConfig httpRequestConfig() {
        var httpConfig = appProperties.http();

        return HttpRequestConfig.builder()
                .userAgent(httpConfig.userAgent())
                .defaultHeaders(httpConfig.defaultHeaders())
                .timeoutSeconds(httpConfig.timeoutSec())
                .maxRetries(httpConfig.maxRetries())
                .retryDelayMs(httpConfig.retryDelayMs())
                .build();
    }

    /**
     * Класс конфигурации HTTP запросов
     */
    @lombok.Builder
    @lombok.Data
    public static class HttpRequestConfig {
        private String userAgent;
        private java.util.Map<String, String> defaultHeaders;
        private Integer timeoutSeconds;
        private Integer maxRetries;
        private Integer retryDelayMs;

        /**
         * Получение всех заголовков включая User-Agent
         */
        public java.util.Map<String, String> getAllHeaders() {
            var headers = new java.util.HashMap<String, String>();

            // Добавляем заголовки по умолчанию
            headers.put("User-Agent", userAgent);
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.put("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Connection", "keep-alive");
            headers.put("Cache-Control", "no-cache");

            // Добавляем пользовательские заголовки
            if (defaultHeaders != null) {
                headers.putAll(defaultHeaders);
            }

            return headers;
        }
    }
}
