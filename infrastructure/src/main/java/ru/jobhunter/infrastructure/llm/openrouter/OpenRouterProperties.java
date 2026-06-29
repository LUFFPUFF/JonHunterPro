package ru.jobhunter.infrastructure.llm.openrouter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "jobhunter.llm.openrouter")
public record OpenRouterProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String primaryModel,
        String fallbackModels,
        String httpReferer,
        String applicationTitle,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int writeTimeoutSeconds
) {

    private static final String DEFAULT_BASE_URL = "https://openrouter.ai";
    private static final String DEFAULT_PRIMARY_MODEL = "openrouter/free";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }

        return baseUrl.trim();
    }

    public String resolvedPrimaryModel() {
        if (primaryModel == null || primaryModel.isBlank()) {
            return DEFAULT_PRIMARY_MODEL;
        }

        return primaryModel.trim();
    }

    public List<String> resolvedFallbackModels() {
        if (fallbackModels == null || fallbackModels.isBlank()) {
            return List.of();
        }

        return Arrays.stream(fallbackModels.split(","))
                .map(String::trim)
                .filter(model -> !model.isBlank())
                .distinct()
                .toList();
    }

    public Duration resolvedConnectTimeout() {
        return Duration.ofSeconds(resolveTimeout(connectTimeoutSeconds));
    }

    public Duration resolvedReadTimeout() {
        return Duration.ofSeconds(resolveTimeout(readTimeoutSeconds));
    }

    public Duration resolvedWriteTimeout() {
        return Duration.ofSeconds(resolveTimeout(writeTimeoutSeconds));
    }

    public void validateForEnabledProvider() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenRouterConfigurationException("OpenRouter API key is not configured");
        }

        String normalizedBaseUrl = resolvedBaseUrl();

        if (!normalizedBaseUrl.startsWith("https://openrouter.ai") && !normalizedBaseUrl.startsWith("http://localhost")) {
            throw new OpenRouterConfigurationException("OpenRouter base URL must use openrouter.ai domain or localhost for tests");
        }
    }

    private int resolveTimeout(int value) {
        if (value <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }

        return value;
    }
}