package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.Objects;

public record AddVacancyToAutoResponseQueueCommand(
        UserId userId,
        VacancySource source,
        String externalVacancyId,
        String vacancyName,
        String employerName,
        String areaName,
        String vacancyUrl
) {

    public AddVacancyToAutoResponseQueueCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");

        externalVacancyId = requireNotBlank(externalVacancyId, "External vacancy id must not be blank");
        vacancyName = requireNotBlank(vacancyName, "Vacancy name must not be blank");

        employerName = normalize(employerName);
        areaName = normalize(areaName);
        vacancyUrl = normalize(vacancyUrl);
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
