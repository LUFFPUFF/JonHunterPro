package ru.jobhunter.core.application.port.out.llm;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record LlmGenerationRequest(
        String useCase,
        List<LlmMessage> messages,
        LlmGenerationOptions options,
        Set<String> excludedProviderIds
) {

    public LlmGenerationRequest(
            String useCase,
            List<LlmMessage> messages,
            LlmGenerationOptions options
    ) {
        this(
                useCase,
                messages,
                options,
                Set.of()
        );
    }

    public LlmGenerationRequest {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM use case must not be blank"
            );
        }

        Objects.requireNonNull(
                messages,
                "LLM messages must not be null"
        );

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "LLM messages must not be empty"
            );
        }

        Objects.requireNonNull(
                options,
                "LLM generation options must not be null"
        );

        Objects.requireNonNull(
                excludedProviderIds,
                "Excluded provider ids must not be null"
        );

        useCase = useCase.trim();
        messages = List.copyOf(messages);
        excludedProviderIds = normalizeProviderIds(
                excludedProviderIds
        );
    }

    public LlmGenerationRequest excludingProvider(
            String providerId
    ) {
        Set<String> updatedProviderIds = new LinkedHashSet<>(
                excludedProviderIds
        );

        updatedProviderIds.add(
                normalizeProviderId(providerId)
        );

        return new LlmGenerationRequest(
                useCase,
                messages,
                options,
                updatedProviderIds
        );
    }

    public boolean excludesProvider(String providerId) {
        return excludedProviderIds.contains(
                normalizeProviderId(providerId)
        );
    }

    private static Set<String> normalizeProviderIds(
            Set<String> providerIds
    ) {
        Set<String> normalizedProviderIds = new LinkedHashSet<>();

        for (String providerId : providerIds) {
            normalizedProviderIds.add(
                    normalizeProviderId(providerId)
            );
        }

        return Set.copyOf(normalizedProviderIds);
    }

    private static String normalizeProviderId(String providerId) {
        Objects.requireNonNull(
                providerId,
                "LLM provider id must not be null"
        );

        String normalizedProviderId = providerId
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedProviderId.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM provider id must not be blank"
            );
        }

        return normalizedProviderId;
    }
}