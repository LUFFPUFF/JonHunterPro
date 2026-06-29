package ru.jobhunter.infrastructure.llm.routing;

import java.util.Optional;

public interface LlmProviderCircuitBreaker {

    Optional<LlmProviderCircuitOpenState> openState(String providerId);

    void recordSuccess(String providerId);

    void recordFailure(LlmProviderUnavailableException failure);
}
