package ru.jobhunter.infrastructure.platform.hh.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhCurrentUserDto;
import ru.jobhunter.core.application.exception.HhAccountNotConnectedException;
import ru.jobhunter.core.application.usecase.integration.GetHhCurrentUserUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhCurrentUserResponse;
import ru.jobhunter.infrastructure.platform.hh.auth.HhOAuthTokenService;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhCurrentUserService implements GetHhCurrentUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(HhCurrentUserService.class);

    private static final Duration TOKEN_REFRESH_SKEW = Duration.ofMinutes(1);

    private final ExternalAuthTokenRepository tokenRepository;
    private final HhOAuthTokenService tokenService;
    private final HhApiClient apiClient;

    public HhCurrentUserService(
            ExternalAuthTokenRepository tokenRepository,
            HhOAuthTokenService tokenService,
            HhApiClient apiClient
    ) {
        this.tokenRepository = Objects.requireNonNull(
                tokenRepository,
                "External auth token repository must not be null"
        );
        this.tokenService = Objects.requireNonNull(
                tokenService,
                "HH OAuth token service must not be null"
        );
        this.apiClient = Objects.requireNonNull(
                apiClient,
                "HH API client must not be null"
        );
    }

    @Override
    public CompletableFuture<HhCurrentUserDto> getCurrentUser(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenRepository.findByUserIdAndProvider(userId, AuthProvider.HH_RU)
                .thenCompose(optionalToken -> {
                    ExternalAuthToken token = optionalToken.orElseThrow(
                            () -> new HhAccountNotConnectedException("HH.ru account is not connected")
                    );

                    if (shouldRefresh(token)) {
                        log.info("HH.ru access token is expired or close to expiration: userId={}", userId);
                        return tokenService.refreshAndSave(userId);
                    }

                    return CompletableFuture.completedFuture(token);
                })
                .thenCompose(token -> apiClient.getCurrentUser(token.accessToken()))
                .thenApply(this::toDto)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Current HH.ru user loaded successfully: userId={}", userId);
                    } else {
                        log.warn("Failed to load current HH.ru user: userId={}", userId, throwable);
                    }
                });
    }

    private boolean shouldRefresh(ExternalAuthToken token) {
        Instant refreshThreshold = Instant.now().plus(TOKEN_REFRESH_SKEW);

        return !token.expiresAt().isAfter(refreshThreshold);
    }

    private HhCurrentUserDto toDto(HhCurrentUserResponse response) {
        return new HhCurrentUserDto(
                AuthProvider.HH_RU,
                response.id(),
                response.email(),
                response.firstName(),
                response.lastName(),
                response.middleName(),
                response.userType(),
                response.admin(),
                Instant.now()
        );
    }
}
