package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class User {

    private final UserId id;
    private final Email email;
    private final PasswordHash passwordHash;
    private final FullName fullName;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "User id must not be null");
        this.email = Objects.requireNonNull(builder.email, "Email must not be null");
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "Password hash must not be null");
        this.fullName = Objects.requireNonNull(builder.fullName, "Full name must not be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created at must not be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "Updated at must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated at must not be before created at");
        }
    }

    public static User register(Email email, PasswordHash passwordHash, FullName fullName) {
        Instant now = Instant.now();

        return builder()
                .id(UserId.newId())
                .email(email)
                .passwordHash(passwordHash)
                .fullName(fullName)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User restore(
            UserId id,
            Email email,
            PasswordHash passwordHash,
            FullName fullName,
            Instant createdAt,
            Instant updatedAt
    ) {
        return builder()
                .id(id)
                .email(email)
                .passwordHash(passwordHash)
                .fullName(fullName)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public User changeFullName(FullName newFullName) {
        return builder()
                .id(id)
                .email(email)
                .passwordHash(passwordHash)
                .fullName(newFullName)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public User changePasswordHash(PasswordHash newPasswordHash) {
        return builder()
                .id(id)
                .email(email)
                .passwordHash(newPasswordHash)
                .fullName(fullName)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public FullName fullName() {
        return fullName;
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

    public static final class Builder {

        private UserId id;
        private Email email;
        private PasswordHash passwordHash;
        private FullName fullName;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(PasswordHash passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder fullName(FullName fullName) {
            this.fullName = fullName;
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

        public User build() {
            return new User(this);
        }
    }
}
