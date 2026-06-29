package ru.jobhunter.infrastructure.llm.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatMessage(
        String role,
        String content,

        @JsonProperty(
                value = "thinking",
                access = JsonProperty.Access.READ_ONLY
        )
        String thinking
) {

    public OllamaChatMessage(
            String role,
            String content
    ) {
        this(role, content, null);
    }
}