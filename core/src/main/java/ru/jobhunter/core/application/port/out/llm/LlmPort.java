package ru.jobhunter.core.application.port.out.llm;

import java.util.concurrent.CompletableFuture;

public interface LlmPort {

    CompletableFuture<LlmGenerationResponse> generate(LlmGenerationRequest request);
}
