package ru.jobhunter.infrastructure.llm.routing;

import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmPort;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class UnavailableLlmPort implements LlmPort {

    @Override
    public CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        return CompletableFuture.failedFuture(
                new LlmProviderUnavailableException(
                        "routing",
                        LlmFailureCategory.NETWORK_UNAVAILABLE,
                        "No LLM provider is enabled"
                )
        );
    }
}