package ru.jobhunter.infrastructure.llm.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class LlmRoutingPort implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(
            LlmRoutingPort.class
    );

    private static final String ROUTING_PROVIDER_ID = "routing";

    private final LlmProvider primaryProvider;
    private final Optional<LlmProvider> fallbackProvider;

    public LlmRoutingPort(
            LlmProvider primaryProvider,
            Optional<LlmProvider> fallbackProvider
    ) {
        this.primaryProvider = Objects.requireNonNull(
                primaryProvider,
                "Primary LLM provider must not be null"
        );
        this.fallbackProvider = Objects.requireNonNull(
                fallbackProvider,
                "Fallback LLM provider optional must not be null"
        );
    }

    @Override
    public CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        List<LlmProvider> eligibleProviders =
                resolveEligibleProviders(request);

        if (eligibleProviders.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new LlmProviderUnavailableException(
                            ROUTING_PROVIDER_ID,
                            LlmFailureCategory.NETWORK_UNAVAILABLE,
                            "No eligible LLM provider is available: "
                                    + "useCase="
                                    + request.useCase()
                    )
            );
        }

        return tryGenerateWithProvider(
                request,
                eligibleProviders,
                0
        );
    }

    private CompletableFuture<LlmGenerationResponse>
    tryGenerateWithProvider(
            LlmGenerationRequest request,
            List<LlmProvider> providers,
            int providerIndex
    ) {
        LlmProvider provider = providers.get(providerIndex);

        return provider.generate(request)
                .exceptionallyCompose(throwable ->
                        tryGenerateWithNextProvider(
                                request,
                                providers,
                                providerIndex,
                                throwable
                        )
                );
    }

    private CompletableFuture<LlmGenerationResponse>
    tryGenerateWithNextProvider(
            LlmGenerationRequest request,
            List<LlmProvider> providers,
            int providerIndex,
            Throwable throwable
    ) {
        Throwable cause = unwrap(throwable);

        if (!(cause instanceof LlmProviderUnavailableException
                unavailableException)) {
            return CompletableFuture.failedFuture(cause);
        }

        int nextProviderIndex = providerIndex + 1;

        if (nextProviderIndex >= providers.size()) {
            return CompletableFuture.failedFuture(cause);
        }

        LlmProvider failedProvider = providers.get(providerIndex);
        LlmProvider nextProvider = providers.get(
                nextProviderIndex
        );

        log.warn(
                "LLM provider is unavailable. Trying next eligible "
                        + "provider: useCase={}, failedProvider={}, "
                        + "nextProvider={}, failureType={}, failureCategory={}, message={}",
                request.useCase(),
                failedProvider.providerId(),
                nextProvider.providerId(),
                cause.getClass().getSimpleName(),
                unavailableException.failureCategory(),
                normalizedMessage(cause)
        );

        return tryGenerateWithProvider(
                request,
                providers,
                nextProviderIndex
        );
    }

    private List<LlmProvider> resolveEligibleProviders(
            LlmGenerationRequest request
    ) {
        List<LlmProvider> providers = new ArrayList<>();

        addIfEligible(
                providers,
                primaryProvider,
                request
        );

        fallbackProvider.ifPresent(provider ->
                addIfEligible(
                        providers,
                        provider,
                        request
                )
        );

        return List.copyOf(providers);
    }

    private void addIfEligible(
            List<LlmProvider> providers,
            LlmProvider provider,
            LlmGenerationRequest request
    ) {
        if (request.excludesProvider(provider.providerId())) {
            log.info(
                    "LLM provider excluded for this generation attempt: "
                            + "useCase={}, provider={}",
                    request.useCase(),
                    provider.providerId()
            );
            return;
        }

        boolean alreadyAdded = providers.stream()
                .anyMatch(existing ->
                        existing.providerId().equalsIgnoreCase(
                                provider.providerId()
                        )
                );

        if (!alreadyAdded) {
            providers.add(provider);
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private String normalizedMessage(Throwable throwable) {
        String message = throwable.getMessage();

        return message == null || message.isBlank()
                ? "No error message"
                : message.trim();
    }
}