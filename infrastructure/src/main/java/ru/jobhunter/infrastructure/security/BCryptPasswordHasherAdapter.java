package ru.jobhunter.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.domain.model.PasswordHash;

import java.util.Objects;

@Component
public final class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "Password encoder must not be null");
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Raw password must not be blank");
        }

        return PasswordHash.of(passwordEncoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        Objects.requireNonNull(passwordHash, "Password hash must not be null");

        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, passwordHash.value());
    }
}
