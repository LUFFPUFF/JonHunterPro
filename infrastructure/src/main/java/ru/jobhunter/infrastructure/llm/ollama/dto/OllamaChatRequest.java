package ru.jobhunter.infrastructure.llm.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaChatMessage> messages,
        boolean stream,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        JsonNode format,

        OllamaChatOptions options,
        boolean think,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("keep_alive")
        String keepAlive
) {

    public OllamaChatRequest(
            String model,
            List<OllamaChatMessage> messages,
            boolean stream,
            JsonNode format,
            OllamaChatOptions options,
            boolean think
    ) {
        this(
                model,
                messages,
                stream,
                format,
                options,
                think,
                null
        );
    }
}