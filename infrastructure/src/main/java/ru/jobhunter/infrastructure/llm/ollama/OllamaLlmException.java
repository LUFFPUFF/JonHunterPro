package ru.jobhunter.infrastructure.llm.ollama;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

public class OllamaLlmException
        extends LlmProviderUnavailableException {

    private static final String PROVIDER = "ollama";

    public OllamaLlmException(
            LlmFailureCategory failureCategory,
            String message
    ) {
        super(PROVIDER, failureCategory, message);
    }

    public OllamaLlmException(
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause
    ) {
        super(PROVIDER, failureCategory, message, cause);
    }
}