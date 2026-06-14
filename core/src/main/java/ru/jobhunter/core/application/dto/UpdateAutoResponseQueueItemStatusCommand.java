package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;

public record UpdateAutoResponseQueueItemStatusCommand(
        UserId userId,
        AutoResponseQueueItemId itemId,
        AutoResponseQueueStatus status
) {

    public UpdateAutoResponseQueueItemStatusCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(itemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(status, "Auto response queue status must not be null");
    }
}
