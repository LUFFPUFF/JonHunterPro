package ru.jobhunter.infrastructure.platform.habr.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HabrCareerConnectionFlowDto;
import ru.jobhunter.core.application.dto.HabrCareerConnectionResultDto;
import ru.jobhunter.core.application.usecase.integration.ConnectHabrCareerAccountUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public final class HabrCareerOAuthConnectionService implements ConnectHabrCareerAccountUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(HabrCareerOAuthConnectionService.class);

    private static final Duration CALLBACK_TIMEOUT = Duration.ofMinutes(5);

    private final HabrCareerOAuthAuthorizationUrlFactory authorizationUrlFactory;
    private final HabrCareerOAuthCustomSchemeCallbackRegistry callbackRegistry;
    private final HabrCareerOAuthTokenService tokenService;

    public HabrCareerOAuthConnectionService(
            HabrCareerOAuthAuthorizationUrlFactory authorizationUrlFactory,
            HabrCareerOAuthCustomSchemeCallbackRegistry callbackRegistry,
            HabrCareerOAuthTokenService tokenService
    ) {
        this.authorizationUrlFactory = Objects.requireNonNull(
                authorizationUrlFactory,
                "Habr Career OAuth authorization URL factory must not be null"
        );
        this.callbackRegistry = Objects.requireNonNull(
                callbackRegistry,
                "Habr Career OAuth custom scheme callback registry must not be null"
        );
        this.tokenService = Objects.requireNonNull(
                tokenService,
                "Habr Career OAuth token service must not be null"
        );
    }

    @Override
    public HabrCareerConnectionFlowDto startConnection(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        HabrCareerOAuthAuthorizationUrlFactory.HabrCareerOAuthAuthorizationUrl authorization =
                authorizationUrlFactory.createAuthorizationUrl();

        CompletableFuture<HabrCareerConnectionResultDto> completion = callbackRegistry
                .waitForCallback(authorization.state(), CALLBACK_TIMEOUT)
                .thenCompose(callbackResult -> tokenService.exchangeAndSave(
                        userId,
                        callbackResult.code()
                ))
                .thenApply(savedToken -> new HabrCareerConnectionResultDto(
                        AuthProvider.HABR_CAREER,
                        Instant.now()
                ))
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Habr Career account connected successfully: userId={}", userId);
                    } else {
                        log.warn("Failed to connect Habr Career account: userId={}", userId, throwable);
                    }
                });

        log.info("Habr Career OAuth connection flow started: userId={}", userId);

        return new HabrCareerConnectionFlowDto(
                authorization.url(),
                authorization.state(),
                completion
        );
    }
}