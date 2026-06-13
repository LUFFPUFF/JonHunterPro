package ru.jobhunter.core.application.usecase.user;

import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.domain.exception.InvalidCredentialsException;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.repository.UserRepository;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public LoginUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher must not be null");
    }

    public CompletableFuture<AuthenticatedUserDto> execute(LoginCommand command) {
        Objects.requireNonNull(command, "Login command must not be null");

        Email email = Email.of(command.email());

        if (command.rawPassword().isBlank()) {
            return CompletableFuture.failedFuture(new InvalidCredentialsException());
        }

        return userRepository.findByEmail(email)
                .thenApply(optionalUser -> {
                    User user = optionalUser.orElseThrow(InvalidCredentialsException::new);

                    boolean passwordMatches = passwordHasher.matches(
                            command.rawPassword(),
                            user.passwordHash()
                    );

                    if (!passwordMatches) {
                        throw new InvalidCredentialsException();
                    }

                    return toDto(user);
                });
    }

    private AuthenticatedUserDto toDto(User user) {
        return new AuthenticatedUserDto(
                user.id().value(),
                user.email().value(),
                user.fullName().value()
        );
    }
}
