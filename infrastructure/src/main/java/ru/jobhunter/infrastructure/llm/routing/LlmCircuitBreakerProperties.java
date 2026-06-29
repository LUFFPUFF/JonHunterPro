package ru.jobhunter.infrastructure.llm.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jobhunter.llm.circuit-breaker")
public record LlmCircuitBreakerProperties(
        int ollamaRuntimeCrashCooldownSeconds,
        int openRouterRateLimitFallbackCooldownSeconds
) {

    private static final int DEFAULT_COOLDOWN_SECONDS = 900;

    public Duration resolvedOllamaRuntimeCrashCooldown() {
        return resolveCooldown(ollamaRuntimeCrashCooldownSeconds);
    }

    public Duration resolvedOpenRouterRateLimitFallbackCooldown() {
        return resolveCooldown(
                openRouterRateLimitFallbackCooldownSeconds
        );
    }

    private Duration resolveCooldown(int configuredSeconds) {
        int seconds = configuredSeconds > 0
                ? configuredSeconds
                : DEFAULT_COOLDOWN_SECONDS;

        return Duration.ofSeconds(seconds);
    }
}