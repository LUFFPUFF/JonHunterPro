package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record HabrCareerVisibleVacancyDto(
        String externalVacancyId,
        String title,
        String vacancyUrl,
        String companyName,
        String city,
        String qualification,
        String workFormat,
        String salary,
        List<String> skills,
        String publishedAt
) {

    public HabrCareerVisibleVacancyDto {
        externalVacancyId = requireNotBlank(
                externalVacancyId,
                "Habr Career external vacancy id must not be blank"
        );
        title = requireNotBlank(
                title,
                "Habr Career vacancy title must not be blank"
        );
        vacancyUrl = requireNotBlank(
                vacancyUrl,
                "Habr Career vacancy URL must not be blank"
        );
        companyName = normalize(companyName);
        city = normalize(city);
        qualification = normalize(qualification);
        workFormat = normalize(workFormat);
        salary = normalize(salary);
        skills = List.copyOf(Objects.requireNonNullElse(skills, List.of()));
        publishedAt = normalize(publishedAt);
    }

    private static String requireNotBlank(String value, String message) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
