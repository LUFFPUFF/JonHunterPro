package ru.jobhunter.infrastructure.llm.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "jobhunter.llm.ollama")
public record OllamaProperties(
        boolean enabled,
        String baseUrl,
        String model,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int writeTimeoutSeconds,
        int contextLength,
        String keepAlive
) {

    private static final String DEFAULT_BASE_URL =
            "http://localhost:11434";

    private static final String DEFAULT_MODEL =
            "qwen3:14b";

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 30;

    public String resolvedBaseUrl() {
        return resolveText(baseUrl, DEFAULT_BASE_URL);
    }

    public String resolvedModel() {
        return resolveText(model, DEFAULT_MODEL);
    }

    public Duration resolvedConnectTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(
                        connectTimeoutSeconds,
                        DEFAULT_CONNECT_TIMEOUT_SECONDS
                )
        );
    }

    public Duration resolvedReadTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(
                        readTimeoutSeconds,
                        DEFAULT_READ_TIMEOUT_SECONDS
                )
        );
    }

    public Duration resolvedWriteTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(
                        writeTimeoutSeconds,
                        DEFAULT_WRITE_TIMEOUT_SECONDS
                )
        );
    }

    public Integer resolvedContextLength() {
        return contextLength > 0
                ? contextLength
                : null;
    }

    public String resolvedKeepAlive() {
        return keepAlive == null || keepAlive.isBlank()
                ? null
                : keepAlive.trim();
    }

    public void validateForEnabledProvider() {
        URI uri;

        try {
            uri = URI.create(resolvedBaseUrl());
        } catch (IllegalArgumentException exception) {
            throw new OllamaConfigurationException(
                    "Ollama base URL is invalid",
                    exception
            );
        }

        if (uri.getScheme() == null
                || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getHost().isBlank()) {
            throw new OllamaConfigurationException(
                    "Ollama base URL must be an HTTP or HTTPS URL"
            );
        }

        if (contextLength < 0) {
            throw new OllamaConfigurationException(
                    "Ollama context length must not be negative"
            );
        }
    }

    private String resolveText(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }

    private int resolveTimeout(
            int value,
            int defaultValue
    ) {
        return value > 0
                ? value
                : defaultValue;
    }
}