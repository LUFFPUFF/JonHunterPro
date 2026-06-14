package ru.jobhunter.core.application.port.out;

import ru.jobhunter.core.domain.model.PasswordHash;

public interface PasswordHasherPort {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash passwordHash);
}
