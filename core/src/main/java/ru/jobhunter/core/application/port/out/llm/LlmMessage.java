package ru.jobhunter.core.application.port.out.llm;

import java.util.Objects;

public record LlmMessage(
        LlmRole role,
        String content
) {

    public LlmMessage {
        Objects.requireNonNull(role, "LLM message role must not be null");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("LLM message content must not be blank");
        }

        content = content.trim();
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(LlmRole.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(LlmRole.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(LlmRole.ASSISTANT, content);
    }
}