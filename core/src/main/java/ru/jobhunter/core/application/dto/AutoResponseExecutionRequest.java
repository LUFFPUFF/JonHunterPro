package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.Objects;

public record AutoResponseExecutionRequest(
        UserId userId,
        AutoResponseQueueItemId queueItemId,
        VacancySource source,
        String externalVacancyId,
        String vacancyName,
        String vacancyUrl
) {

    public AutoResponseExecutionRequest {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(queueItemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");

        externalVacancyId = requireNotBlank(externalVacancyId, "External vacancy id must not be blank");
        vacancyName = requireNotBlank(vacancyName, "Vacancy name must not be blank");
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
