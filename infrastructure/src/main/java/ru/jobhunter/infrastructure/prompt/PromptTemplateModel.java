package ru.jobhunter.infrastructure.prompt;

import java.util.Map;

@FunctionalInterface
public interface PromptTemplateModel {

    Map<String, Object> toTemplateModel();

    static PromptTemplateModel empty() {
        return Map::of;
    }
}