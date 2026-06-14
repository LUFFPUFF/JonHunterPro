package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public record AutoResponseQueueItem(
        AutoResponseQueueItemId id,
        UserId userId,
        VacancySource source,
        String externalVacancyId,
        String vacancyName,
        String employerName,
        String areaName,
        String vacancyUrl,
        AutoResponseQueueStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public AutoResponseQueueItem {
        Objects.requireNonNull(id, "Auto response queue item id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");
        Objects.requireNonNull(status, "Auto response queue status must not be null");
        Objects.requireNonNull(createdAt, "Created timestamp must not be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp must not be null");

        externalVacancyId = requireNotBlank(externalVacancyId, "External vacancy id must not be blank");
        vacancyName = requireNotBlank(vacancyName, "Vacancy name must not be blank");

        employerName = normalize(employerName);
        areaName = normalize(areaName);
        vacancyUrl = normalize(vacancyUrl);
    }

    public static AutoResponseQueueItem create(
            UserId userId,
            VacancySource source,
            String externalVacancyId,
            String vacancyName,
            String employerName,
            String areaName,
            String vacancyUrl
    ) {
        Instant now = Instant.now();

        return new AutoResponseQueueItem(
                AutoResponseQueueItemId.newId(),
                userId,
                source,
                externalVacancyId,
                vacancyName,
                employerName,
                areaName,
                vacancyUrl,
                AutoResponseQueueStatus.QUEUED,
                now,
                now
        );
    }

    public AutoResponseQueueItem withStatus(AutoResponseQueueStatus newStatus) {
        Objects.requireNonNull(newStatus, "Auto response queue status must not be null");

        return new AutoResponseQueueItem(
                id,
                userId,
                source,
                externalVacancyId,
                vacancyName,
                employerName,
                areaName,
                vacancyUrl,
                newStatus,
                createdAt,
                Instant.now()
        );
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
