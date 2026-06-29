package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record HhVacancyDetailsDto(
        String externalId,
        String name,
        String employerName,
        String areaName,
        String vacancyUrl,
        String description,
        List<String> keySkills,
        String experienceName,
        String employmentName,
        String scheduleName,
        boolean responseLetterRequired
) {

    public HhVacancyDetailsDto {
        externalId = requireNotBlank(
                externalId,
                "HH vacancy external id must not be blank"
        );

        name = requireNotBlank(
                name,
                "HH vacancy name must not be blank"
        );

        description = requireNotBlank(
                description,
                "HH vacancy description must not be blank"
        );

        employerName = normalize(employerName);
        areaName = normalize(areaName);
        vacancyUrl = normalize(vacancyUrl);
        experienceName = normalize(experienceName);
        employmentName = normalize(employmentName);
        scheduleName = normalize(scheduleName);

        keySkills = keySkills == null
                ? List.of()
                : keySkills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}