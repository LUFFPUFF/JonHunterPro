package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record HabrCareerVacancyDetailsDto(
        String externalVacancyId,
        String title,
        String vacancyUrl,
        String companyName,
        String description,
        List<String> skills,
        String city,
        String employmentType,
        String salary,
        String publishedAt,
        String validThrough,
        boolean responseActionAvailable
) {

    public HabrCareerVacancyDetailsDto {
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
        description = normalize(description);
        skills = List.copyOf(Objects.requireNonNullElse(skills, List.of()));
        city = normalize(city);
        employmentType = normalize(employmentType);
        salary = normalize(salary);
        publishedAt = normalize(publishedAt);
        validThrough = normalize(validThrough);
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
