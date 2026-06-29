package ru.jobhunter.core.application.port.out.llm;

public record LlmUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {

    public LlmUsage {
        if (promptTokens < 0) {
            throw new IllegalArgumentException("Prompt tokens count must not be negative");
        }

        if (completionTokens < 0) {
            throw new IllegalArgumentException("Completion tokens count must not be negative");
        }

        if (totalTokens < 0) {
            throw new IllegalArgumentException("Total tokens count must not be negative");
        }
    }

    public static LlmUsage unknown() {
        return new LlmUsage(0, 0, 0);
    }
}