package ru.jobhunter.core.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ResumeId(UUID value) {

    public ResumeId {
        Objects.requireNonNull(value, "Resume id must not be null");
    }

    public static ResumeId newId() {
        return new ResumeId(UUID.randomUUID());
    }

    public static ResumeId of(UUID value) {
        return new ResumeId(value);
    }
}