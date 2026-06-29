package ru.jobhunter.infrastructure.llm.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaChatOptions(
        double temperature,

        @JsonProperty("num_predict")
        int numPredict,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("num_ctx")
        Integer numCtx
) {

    public OllamaChatOptions(
            double temperature,
            int numPredict
    ) {
        this(
                temperature,
                numPredict,
                null
        );
    }
}