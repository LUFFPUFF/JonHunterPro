package ru.jobhunter.infrastructure.platform.habr.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HabrCareerCurrentUserDto;
import ru.jobhunter.core.application.exception.HabrCareerAccountNotConnectedException;
import ru.jobhunter.core.application.usecase.integration.GetHabrCareerCurrentUserUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;
import ru.jobhunter.infrastructure.platform.habr.api.dto.HabrCareerCurrentUserResponse;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public final class HabrCareerCurrentUserService implements GetHabrCareerCurrentUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(HabrCareerCurrentUserService.class);

    private final ExternalAuthTokenRepository tokenRepository;
    private final HabrCareerApiClient apiClient;

    public HabrCareerCurrentUserService(
            ExternalAuthTokenRepository tokenRepository,
            HabrCareerApiClient apiClient
    ) {
        this.tokenRepository = Objects.requireNonNull(
                tokenRepository,
                "External auth token repository must not be null"
        );
        this.apiClient = Objects.requireNonNull(
                apiClient,
                "Habr Career API client must not be null"
        );
    }

    @Override
    public CompletableFuture<HabrCareerCurrentUserDto> getCurrentUser(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenRepository.findByUserIdAndProvider(userId, AuthProvider.HABR_CAREER)
                .thenCompose(optionalToken -> {
                    ExternalAuthToken token = optionalToken.orElseThrow(
                            () -> new HabrCareerAccountNotConnectedException(
                                    "Habr Career account is not connected"
                            )
                    );

                    return apiClient.getCurrentUser(token.accessToken());
                })
                .thenApply(this::toDto)
                .whenComplete((currentUser, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Habr Career current user loaded: userId={}, login={}",
                                userId,
                                currentUser.login()
                        );
                    } else {
                        log.warn(
                                "Failed to load Habr Career current user: userId={}",
                                userId,
                                throwable
                        );
                    }
                });
    }

    private HabrCareerCurrentUserDto toDto(HabrCareerCurrentUserResponse response) {
        HabrCareerCurrentUserResponse.Location location = response.location();

        return new HabrCareerCurrentUserDto(
                AuthProvider.HABR_CAREER,
                response.login(),
                response.email(),
                response.firstName(),
                response.lastName(),
                response.middleName(),
                response.birthday(),
                response.avatar(),
                location == null ? null : location.city(),
                location == null ? null : location.country(),
                response.gender(),
                Instant.now()
        );
    }
}