package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.UUID;

public record AutoResponseQueueItemDto(
        UUID id,
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
}
