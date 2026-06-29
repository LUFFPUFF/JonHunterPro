package ru.jobhunter.infrastructure.llm.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(
        String model,
        OllamaChatMessage message,
        boolean done,

        @JsonProperty("done_reason")
        String doneReason,

        @JsonProperty("prompt_eval_count")
        Integer promptEvalCount,

        @JsonProperty("eval_count")
        Integer evalCount,

        @JsonProperty("total_duration")
        Long totalDuration,

        @JsonProperty("load_duration")
        Long loadDuration,

        @JsonProperty("eval_duration")
        Long evalDuration
) {
}