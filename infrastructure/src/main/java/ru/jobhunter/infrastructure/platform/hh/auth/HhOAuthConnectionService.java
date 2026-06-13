package ru.jobhunter.infrastructure.platform.hh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhConnectionFlowDto;
import ru.jobhunter.core.application.dto.HhConnectionResultDto;
import ru.jobhunter.core.application.usecase.integration.ConnectHhAccountUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhOAuthConnectionService implements ConnectHhAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthConnectionService.class);

    private final HhOAuthStateGenerator stateGenerator;
    private final HhOAuthAuthorizationUrlFactory authorizationUrlFactory;
    private final HhOAuthCallbackServer callbackServer;
    private final HhOAuthTokenService tokenService;

    public HhOAuthConnectionService(
            HhOAuthStateGenerator stateGenerator,
            HhOAuthAuthorizationUrlFactory authorizationUrlFactory,
            HhOAuthCallbackServer callbackServer,
            HhOAuthTokenService tokenService
    ) {
        this.stateGenerator = Objects.requireNonNull(stateGenerator, "HH OAuth state generator must not be null");
        this.authorizationUrlFactory = Objects.requireNonNull(
                authorizationUrlFactory,
                "HH OAuth authorization URL factory must not be null"
        );
        this.callbackServer = Objects.requireNonNull(callbackServer, "HH OAuth callback server must not be null");
        this.tokenService = Objects.requireNonNull(tokenService, "HH OAuth token service must not be null");
    }

    @Override
    public HhConnectionFlowDto startConnection(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        HhOAuthAuthorizationUrlFactory.HhOAuthAuthorizationUrl authorization =
                authorizationUrlFactory.createAuthorizationUrl();

        CompletableFuture<HhConnectionResultDto> completion = callbackServer.waitForCallback(authorization.state())
                .thenCompose(callbackResult -> tokenService.exchangeAndSave(userId, callbackResult.code()))
                .thenApply(savedToken -> new HhConnectionResultDto(
                        AuthProvider.HH_RU,
                        Instant.now()
                ))
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("HH.ru account connected successfully: userId={}", userId);
                    } else {
                        log.warn("Failed to connect HH.ru account: userId={}", userId, throwable);
                    }
                });

        log.info("HH.ru OAuth connection flow started: userId={}", userId);

        return new HhConnectionFlowDto(authorization.url(), completion);
    }
}
