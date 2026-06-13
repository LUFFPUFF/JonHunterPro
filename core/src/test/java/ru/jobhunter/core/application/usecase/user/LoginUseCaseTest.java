package ru.jobhunter.core.application.usecase.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.domain.exception.InvalidCredentialsException;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.FullName;
import ru.jobhunter.core.domain.model.PasswordHash;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final String PASSWORD_HASH =
            "$2a$12$abcdefghijklmnopqrstuu123456789012345678901234567890";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);

    private final LoginUseCase useCase = new LoginUseCase(
            userRepository,
            passwordHasher
    );

    @Test
    void shouldLoginExistingUser() {
        Email email = Email.of("test@example.com");
        PasswordHash passwordHash = PasswordHash.of(PASSWORD_HASH);

        User user = User.register(
                email,
                passwordHash,
                FullName.of("Ivan Ivanov")
        );

        when(userRepository.findByEmail(email))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(user)));

        when(passwordHasher.matches("Password123", passwordHash))
                .thenReturn(true);

        var result = useCase.execute(new LoginCommand("test@example.com", "Password123")).join();

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.fullName()).isEqualTo("Ivan Ivanov");
        assertThat(result.id()).isEqualTo(user.id().value());
    }

    @Test
    void shouldFailWhenUserNotFound() {
        Email email = Email.of("missing@example.com");

        when(userRepository.findByEmail(email))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("missing@example.com", "Password123")).join())
                .hasCauseInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldFailWhenPasswordDoesNotMatch() {
        Email email = Email.of("test@example.com");
        PasswordHash passwordHash = PasswordHash.of(PASSWORD_HASH);

        User user = User.register(
                email,
                passwordHash,
                FullName.of("Ivan Ivanov")
        );

        when(userRepository.findByEmail(email))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(user)));

        when(passwordHasher.matches("WrongPassword123", passwordHash))
                .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("test@example.com", "WrongPassword123")).join())
                .hasCauseInstanceOf(InvalidCredentialsException.class);
    }
}