package ru.jobhunter.infrastructure.llm.openrouter.dto;

public record OpenRouterResponseFormat(String type) {
    public static OpenRouterResponseFormat jsonObject() {
        return new OpenRouterResponseFormat("json_object");
    }
}
