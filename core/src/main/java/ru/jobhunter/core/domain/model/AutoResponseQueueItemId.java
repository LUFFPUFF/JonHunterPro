package ru.jobhunter.core.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AutoResponseQueueItemId(UUID value) {

    public AutoResponseQueueItemId {
        Objects.requireNonNull(value, "Auto response queue item id must not be null");
    }

    public static AutoResponseQueueItemId newId() {
        return new AutoResponseQueueItemId(UUID.randomUUID());
    }

    public static AutoResponseQueueItemId of(UUID value) {
        return new AutoResponseQueueItemId(value);
    }
}
