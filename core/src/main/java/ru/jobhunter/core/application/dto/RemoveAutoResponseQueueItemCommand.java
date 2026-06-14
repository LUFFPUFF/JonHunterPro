package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;

public record RemoveAutoResponseQueueItemCommand(
        UserId userId,
        AutoResponseQueueItemId itemId
) {

    public RemoveAutoResponseQueueItemCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(itemId, "Auto response queue item id must not be null");
    }
}
