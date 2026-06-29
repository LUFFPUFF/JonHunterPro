package ru.jobhunter.infrastructure.prompt;

public class PromptTemplateRenderingException
        extends RuntimeException {

    public PromptTemplateRenderingException(
            String message
    ) {
        super(message);
    }

    public PromptTemplateRenderingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}