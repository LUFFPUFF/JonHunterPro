package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AuthProvider;

import java.time.Instant;
import java.util.Objects;

public record HhConnectionStatusDto(
        AuthProvider provider,
        Status status,
        Instant expiresAt,
        Instant checkedAt
) {

    public HhConnectionStatusDto {
        Objects.requireNonNull(provider, "Auth provider must not be null");
        Objects.requireNonNull(status, "HH connection status must not be null");
        Objects.requireNonNull(checkedAt, "Checked timestamp must not be null");
    }

    public static HhConnectionStatusDto disconnected(Instant checkedAt) {
        return new HhConnectionStatusDto(
                AuthProvider.HH_RU,
                Status.DISCONNECTED,
                null,
                checkedAt
        );
    }

    public static HhConnectionStatusDto connected(Instant expiresAt, Instant checkedAt) {
        Objects.requireNonNull(expiresAt, "Expiration timestamp must not be null");

        return new HhConnectionStatusDto(
                AuthProvider.HH_RU,
                Status.CONNECTED,
                expiresAt,
                checkedAt
        );
    }

    public static HhConnectionStatusDto expired(Instant expiresAt, Instant checkedAt) {
        Objects.requireNonNull(expiresAt, "Expiration timestamp must not be null");

        return new HhConnectionStatusDto(
                AuthProvider.HH_RU,
                Status.EXPIRED,
                expiresAt,
                checkedAt
        );
    }

    public boolean isConnected() {
        return status == Status.CONNECTED;
    }

    public boolean isExpired() {
        return status == Status.EXPIRED;
    }

    public enum Status {
        DISCONNECTED,
        CONNECTED,
        EXPIRED
    }
}
