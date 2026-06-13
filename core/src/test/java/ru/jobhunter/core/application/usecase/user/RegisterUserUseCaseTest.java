package ru.jobhunter.core.application.usecase.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.domain.exception.UserAlreadyExistsException;
import ru.jobhunter.core.domain.exception.WeakPasswordException;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.PasswordHash;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    private static final String PASSWORD_HASH =
            "$2a$12$abcdefghijklmnopqrstuu123456789012345678901234567890";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);

    private final RegisterUserUseCase useCase = new RegisterUserUseCase(
            userRepository,
            passwordHasher
    );

    @Test
    void shouldRegisterNewUser() {
        var command = new RegisterUserCommand(
                "Test@Example.com",
                "Password123",
                "Ivan Ivanov"
        );

        when(userRepository.existsByEmail(Email.of("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(false));

        when(passwordHasher.hash("Password123"))
                .thenReturn(PasswordHash.of(PASSWORD_HASH));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));

        var result = useCase.execute(command).join();

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.fullName()).isEqualTo("Ivan Ivanov");
        assertThat(result.id()).isNotNull();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailWhenUserAlreadyExists() {
        var command = new RegisterUserCommand(
                "test@example.com",
                "Password123",
                "Ivan Ivanov"
        );

        when(userRepository.existsByEmail(Email.of("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertThatThrownBy(() -> useCase.execute(command).join())
                .hasCauseInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void shouldFailWhenPasswordIsWeak() {
        var command = new RegisterUserCommand(
                "test@example.com",
                "password",
                "Ivan Ivanov"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(WeakPasswordException.class);
    }

}