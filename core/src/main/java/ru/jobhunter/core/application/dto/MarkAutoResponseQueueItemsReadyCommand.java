package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.UserId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record MarkAutoResponseQueueItemsReadyCommand(
        UserId userId,
        List<AutoResponseQueueItemId> itemIds
) {

    public MarkAutoResponseQueueItemsReadyCommand {
        Objects.requireNonNull(userId, "User id must not be null");

        itemIds = List.copyOf(
                Objects.requireNonNull(
                        itemIds,
                        "Queue item ids must not be null"
                )
        );

        if (itemIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one queue item must be selected"
            );
        }

        Set<AutoResponseQueueItemId> uniqueIds = new HashSet<>();

        for (AutoResponseQueueItemId itemId : itemIds) {
            Objects.requireNonNull(
                    itemId,
                    "Queue item id must not be null"
            );

            if (!uniqueIds.add(itemId)) {
                throw new IllegalArgumentException(
                        "Duplicate queue item id in batch: "
                                + itemId.value()
                );
            }
        }
    }
}