package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;

public record ExecuteAutoResponseCommand(
        UserId userId,
        AutoResponseQueueItemId queueItemId
) {

    public ExecuteAutoResponseCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(queueItemId, "Auto response queue item id must not be null");
    }
}
