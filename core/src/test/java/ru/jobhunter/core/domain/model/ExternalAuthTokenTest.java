package ru.jobhunter.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalAuthTokenTest {

    @Test
    void shouldRefreshTokenValuesKeepingIdentity() {
        UserId userId = UserId.newId();
        Instant oldExpiresAt = Instant.now().plusSeconds(60);

        ExternalAuthToken token = ExternalAuthToken.create(
                userId,
                AuthProvider.HH_RU,
                "old-access-token",
                "old-refresh-token",
                "bearer",
                "employer",
                oldExpiresAt
        );

        Instant newExpiresAt = Instant.now().plusSeconds(3600);

        ExternalAuthToken refreshed = token.refresh(
                "new-access-token",
                "new-refresh-token",
                "bearer",
                "employer",
                newExpiresAt
        );

        assertThat(refreshed.id()).isEqualTo(token.id());
        assertThat(refreshed.userId()).isEqualTo(token.userId());
        assertThat(refreshed.provider()).isEqualTo(token.provider());
        assertThat(refreshed.createdAt()).isEqualTo(token.createdAt());

        assertThat(refreshed.accessToken()).isEqualTo("new-access-token");
        assertThat(refreshed.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(refreshed.expiresAt()).isEqualTo(newExpiresAt);
        assertThat(refreshed.updatedAt()).isAfterOrEqualTo(token.updatedAt());
    }
}