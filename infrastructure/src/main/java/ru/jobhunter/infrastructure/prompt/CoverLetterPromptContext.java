package ru.jobhunter.infrastructure.prompt;

import java.util.Map;
import java.util.Objects;

public record CoverLetterPromptContext(
        String vacancySource,
        String vacancyId,
        String vacancyTitle,
        String companyName,
        String vacancyUrl,
        String vacancyDescription,
        String resumeText
) implements PromptTemplateModel {

    public CoverLetterPromptContext {
        vacancySource = requireText(
                vacancySource,
                "Vacancy source must not be blank"
        );
        vacancyId = requireText(
                vacancyId,
                "Vacancy id must not be blank"
        );
        vacancyTitle = requireText(
                vacancyTitle,
                "Vacancy title must not be blank"
        );
        companyName = requireText(
                companyName,
                "Company name must not be blank"
        );
        vacancyUrl = requireText(
                vacancyUrl,
                "Vacancy URL must not be blank"
        );
        vacancyDescription = requireText(
                vacancyDescription,
                "Vacancy description must not be blank"
        );
        resumeText = requireText(
                resumeText,
                "Resume text must not be blank"
        );
    }

    @Override
    public Map<String, Object> toTemplateModel() {
        return Map.of(
                "vacancy_source", vacancySource,
                "vacancy_id", vacancyId,
                "vacancy_title", vacancyTitle,
                "company_name", companyName,
                "vacancy_url", vacancyUrl,
                "vacancy_description", vacancyDescription,
                "resume_text", resumeText
        );
    }

    private static String requireText(
            String value,
            String message
    ) {
        Objects.requireNonNull(value, message);

        String normalized = value.strip();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }
}