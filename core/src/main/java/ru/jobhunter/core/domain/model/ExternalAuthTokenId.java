package ru.jobhunter.core.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ExternalAuthTokenId(UUID value) {

    public ExternalAuthTokenId {
        Objects.requireNonNull(value, "External auth token id must not be null");
    }

    public static ExternalAuthTokenId newId() {
        return new ExternalAuthTokenId(UUID.randomUUID());
    }

    public static ExternalAuthTokenId of(UUID value) {
        return new ExternalAuthTokenId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
