package ru.jobhunter.core.application.exception;

public final class QuestionnaireAnswerGenerationUnavailableException
        extends RuntimeException {

    public QuestionnaireAnswerGenerationUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}