package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseExecutionStatus;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.Objects;

public record AutoResponseExecutionResultDto(
        AutoResponseQueueItemId queueItemId,
        VacancySource source,
        String externalVacancyId,
        AutoResponseExecutionStatus status,
        String message,
        Instant executedAt
) {

    public AutoResponseExecutionResultDto {
        Objects.requireNonNull(queueItemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");
        Objects.requireNonNull(status, "Auto response execution status must not be null");
        Objects.requireNonNull(executedAt, "Executed timestamp must not be null");

        externalVacancyId = requireNotBlank(externalVacancyId, "External vacancy id must not be blank");
        message = normalize(message);
    }

    public static AutoResponseExecutionResultDto success(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.SUCCESS,
                message,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto failed(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.FAILED,
                message,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto notAvailable(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.NOT_AVAILABLE,
                message,
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
