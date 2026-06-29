package ru.jobhunter.infrastructure.llm.routing;

import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;

import java.util.concurrent.CompletableFuture;

public interface LlmProvider {

    String providerId();

    CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    );
}