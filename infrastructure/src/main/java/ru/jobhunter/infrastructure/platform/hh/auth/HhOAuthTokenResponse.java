package ru.jobhunter.infrastructure.platform.hh.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record HhOAuthTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        String scope
) {

    public HhOAuthTokenResponse {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("HH access token must not be blank");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("HH refresh token must not be blank");
        }

        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("HH token type must not be blank");
        }

        if (expiresIn <= 0) {
            throw new IllegalArgumentException("HH token expires_in must be positive");
        }

        accessToken = accessToken.trim();
        refreshToken = refreshToken.trim();
        tokenType = tokenType.trim();

        if (scope != null && scope.isBlank()) {
            scope = null;
        }

        Objects.requireNonNull(accessToken, "HH access token must not be null");
        Objects.requireNonNull(refreshToken, "HH refresh token must not be null");
        Objects.requireNonNull(tokenType, "HH token type must not be null");
    }
}
