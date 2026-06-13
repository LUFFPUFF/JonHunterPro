package ru.jobhunter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.core.application.port.out.PasswordHasherPort;
import ru.jobhunter.core.application.usecase.user.LoginUseCase;
import ru.jobhunter.core.application.usecase.user.RegisterUserUseCase;
import ru.jobhunter.core.domain.repository.UserRepository;

@Configuration
public class ApplicationUseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher
    ) {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher
    ) {
        return new LoginUseCase(userRepository, passwordHasher);
    }
}
