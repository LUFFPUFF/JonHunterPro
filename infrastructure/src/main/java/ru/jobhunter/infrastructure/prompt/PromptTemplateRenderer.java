package ru.jobhunter.infrastructure.prompt;

public interface PromptTemplateRenderer {

    String render(
            PromptTemplate template,
            PromptTemplateModel model
    );
}