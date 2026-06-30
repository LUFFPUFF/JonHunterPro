package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record ReturnAutoResponseQueueItemsToQueuedResultDto(
        int requestedCount,
        int returnedToQueuedCount,
        int alreadyQueuedCount,
        int notEligibleCount,
        int notFoundCount,
        List<String> failures
) {

    public ReturnAutoResponseQueueItemsToQueuedResultDto {
        if (requestedCount < 1) {
            throw new IllegalArgumentException(
                    "Requested count must be greater than 0"
            );
        }

        if (returnedToQueuedCount < 0
                || alreadyQueuedCount < 0
                || notEligibleCount < 0
                || notFoundCount < 0) {
            throw new IllegalArgumentException(
                    "Result counts must not be negative"
            );
        }

        failures = List.copyOf(
                Objects.requireNonNull(
                        failures,
                        "Failures must not be null"
                )
        );

        int processedCount = returnedToQueuedCount
                + alreadyQueuedCount
                + notEligibleCount
                + notFoundCount
                + failures.size();

        if (processedCount != requestedCount) {
            throw new IllegalArgumentException(
                    "Result counts do not match requested count"
            );
        }
    }

    public int failedCount() {
        return failures.size();
    }
}
