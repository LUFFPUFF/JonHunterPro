package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public class ExternalAuthToken {

    private final ExternalAuthTokenId id;
    private final UserId userId;
    private final AuthProvider provider;
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final String scope;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ExternalAuthToken(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "External auth token id must not be null");
        this.userId = Objects.requireNonNull(builder.userId, "User id must not be null");
        this.provider = Objects.requireNonNull(builder.provider, "Auth provider must not be null");
        this.accessToken = requireNotBlank(builder.accessToken, "Access token must not be blank");
        this.refreshToken = normalizeNullable(builder.refreshToken);
        this.tokenType = requireNotBlank(builder.tokenType, "Token type must not be blank");
        this.scope = normalizeNullable(builder.scope);
        this.expiresAt = builder.expiresAt;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "Updated at must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated at must not be before created at");
        }
    }

    public ExternalAuthToken refresh(
            String newAccessToken,
            String newRefreshToken,
            String newTokenType,
            String newScope,
            Instant newExpiresAt
    ) {
        return builder()
                .id(id)
                .userId(userId)
                .provider(provider)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType(newTokenType)
                .scope(newScope)
                .expiresAt(newExpiresAt)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public static ExternalAuthToken create(
            UserId userId,
            AuthProvider provider,
            String accessToken,
            String refreshToken,
            String tokenType,
            String scope,
            Instant expiresAt
    ) {
        Instant now = Instant.now();

        return builder()
                .id(ExternalAuthTokenId.newId())
                .userId(userId)
                .provider(provider)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .scope(scope)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static ExternalAuthToken createPermanent(
            UserId userId,
            AuthProvider provider,
            String accessToken,
            String tokenType,
            String scope
    ) {
        Instant now = Instant.now();

        return builder()
                .id(ExternalAuthTokenId.newId())
                .userId(userId)
                .provider(provider)
                .accessToken(accessToken)
                .refreshToken(null)
                .tokenType(tokenType)
                .scope(scope)
                .expiresAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static ExternalAuthToken restore(
            ExternalAuthTokenId id,
            UserId userId,
            AuthProvider provider,
            String accessToken,
            String refreshToken,
            String tokenType,
            String scope,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return builder()
                .id(id)
                .userId(userId)
                .provider(provider)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .scope(scope)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "Now instant must not be null");

        if (expiresAt == null) {
            return false;
        }

        return !expiresAt.isAfter(now);
    }

    public ExternalAuthTokenId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public AuthProvider provider() {
        return provider;
    }

    public String accessToken() {
        return accessToken;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public String tokenType() {
        return tokenType;
    }

    public String scope() {
        return scope;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public static final class Builder {

        private ExternalAuthTokenId id;
        private UserId userId;
        private AuthProvider provider;
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private String scope;
        private Instant expiresAt;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(ExternalAuthTokenId id) {
            this.id = id;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder provider(AuthProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ExternalAuthToken build() {
            return new ExternalAuthToken(this);
        }
    }
}
