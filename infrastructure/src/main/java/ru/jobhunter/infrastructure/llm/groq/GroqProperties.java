package ru.jobhunter.infrastructure.llm.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jobhunter.llm.groq")
public record GroqProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        String reasoningEffort,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int writeTimeoutSeconds
) {

    private static final String DEFAULT_BASE_URL =
            "https://api.groq.com/openai/v1";

    private static final String DEFAULT_MODEL =
            "openai/gpt-oss-120b";

    private static final String DEFAULT_REASONING_EFFORT = "low";

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public String resolvedBaseUrl() {
        return normalizeOrDefault(baseUrl, DEFAULT_BASE_URL);
    }

    public String resolvedModel() {
        return normalizeOrDefault(model, DEFAULT_MODEL);
    }

    public String resolvedReasoningEffort() {
        return normalizeOrDefault(
                reasoningEffort,
                DEFAULT_REASONING_EFFORT
        );
    }

    public Duration resolvedConnectTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(connectTimeoutSeconds)
        );
    }

    public Duration resolvedReadTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(readTimeoutSeconds)
        );
    }

    public Duration resolvedWriteTimeout() {
        return Duration.ofSeconds(
                resolveTimeout(writeTimeoutSeconds)
        );
    }

    public void validateForEnabledProvider() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GroqConfigurationException(
                    "Groq API key is not configured"
            );
        }

        String normalizedBaseUrl = resolvedBaseUrl();

        if (!normalizedBaseUrl.startsWith("https://api.groq.com")
                && !normalizedBaseUrl.startsWith("http://localhost")) {
            throw new GroqConfigurationException(
                    "Groq base URL must use api.groq.com "
                            + "or localhost for tests"
            );
        }

        if (resolvedModel().isBlank()) {
            throw new GroqConfigurationException(
                    "Groq model must not be blank"
            );
        }
    }

    private static String normalizeOrDefault(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private static int resolveTimeout(int value) {
        return value <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : value;
    }
}