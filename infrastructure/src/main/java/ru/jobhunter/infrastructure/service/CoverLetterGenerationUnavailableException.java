package ru.jobhunter.infrastructure.service;

import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

public final class CoverLetterGenerationUnavailableException
        extends LlmProviderUnavailableException {

    private static final String PROVIDER_ID =
            "cover-letter-generation";

    public CoverLetterGenerationUnavailableException(
            String message
    ) {
        super(
                PROVIDER_ID,
                LlmFailureCategory.INVALID_MODEL_OUTPUT,
                message
        );
    }

    public CoverLetterGenerationUnavailableException(
            String message,
            Throwable cause
    ) {
        super(
                PROVIDER_ID,
                LlmFailureCategory.INVALID_MODEL_OUTPUT,
                message,
                cause
        );
    }
}