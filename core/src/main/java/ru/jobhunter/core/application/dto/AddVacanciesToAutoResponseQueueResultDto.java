package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record AddVacanciesToAutoResponseQueueResultDto(
        int requestedCount,
        int addedCount,
        int alreadyExistsCount,
        List<AddVacancyToAutoResponseQueueFailureDto> failures
) {

    public AddVacanciesToAutoResponseQueueResultDto {
        if (requestedCount < 1) {
            throw new IllegalArgumentException(
                    "Requested vacancy count must be greater than 0"
            );
        }

        if (addedCount < 0 || alreadyExistsCount < 0) {
            throw new IllegalArgumentException(
                    "Batch result counts must not be negative"
            );
        }

        failures = List.copyOf(
                Objects.requireNonNull(
                        failures,
                        "Failures must not be null"
                )
        );

        int processedCount = addedCount
                + alreadyExistsCount
                + failures.size();

        if (processedCount != requestedCount) {
            throw new IllegalArgumentException(
                    "Batch result counts do not match requested count"
            );
        }
    }

    public int failedCount() {
        return failures.size();
    }
}