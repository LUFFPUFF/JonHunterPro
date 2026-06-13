package ru.jobhunter.core.application.usecase.user;

import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.domain.exception.UserAlreadyExistsException;
import ru.jobhunter.core.domain.exception.WeakPasswordException;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.FullName;
import ru.jobhunter.core.domain.model.PasswordHash;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.repository.UserRepository;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RegisterUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public RegisterUserUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher must not be null");
    }

    public CompletableFuture<AuthenticatedUserDto> execute(RegisterUserCommand command) {
        Objects.requireNonNull(command, "Register user command must not be null");

        Email email = Email.of(command.email());
        FullName fullName = FullName.of(command.fullName());

        validatePassword(command.rawPassword());

        return userRepository.existsByEmail(email)
                .thenCompose(exists -> {
                    if (exists) {
                        return CompletableFuture.failedFuture(
                                new UserAlreadyExistsException(email.value())
                        );
                    }

                    PasswordHash passwordHash = passwordHasher.hash(command.rawPassword());
                    User user = User.register(email, passwordHash, fullName);

                    return userRepository.save(user);
                })
                .thenApply(this::toDto);
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword.isBlank()) {
            throw new WeakPasswordException("Password must not be blank");
        }

        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException(
                    "Password must contain at least " + MIN_PASSWORD_LENGTH + " characters"
            );
        }

        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new WeakPasswordException(
                    "Password must not exceed " + MAX_PASSWORD_LENGTH + " characters"
            );
        }

        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new WeakPasswordException("Password must contain letters and digits");
        }
    }

    private AuthenticatedUserDto toDto(User user) {
        return new AuthenticatedUserDto(
                user.id().value(),
                user.email().value(),
                user.fullName().value()
        );
    }

}
