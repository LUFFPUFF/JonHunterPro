package ru.jobhunter.core.application.dto;

import java.util.Objects;

public record AddVacancyToAutoResponseQueueResultDto(
        AutoResponseQueueItemDto item,
        boolean created
) {

    public AddVacancyToAutoResponseQueueResultDto {
        Objects.requireNonNull(item, "Auto response queue item must not be null");
    }
}
