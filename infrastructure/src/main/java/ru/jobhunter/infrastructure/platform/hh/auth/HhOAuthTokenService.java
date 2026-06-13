package ru.jobhunter.infrastructure.platform.hh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhOAuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthTokenService.class);

    private final HhOAuthTokenClient tokenClient;
    private final ExternalAuthTokenRepository tokenRepository;

    public HhOAuthTokenService(
            HhOAuthTokenClient tokenClient,
            ExternalAuthTokenRepository tokenRepository
    ) {
        this.tokenClient = Objects.requireNonNull(tokenClient, "HH OAuth token client must not be null");
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "Token repository must not be null");
    }

    public CompletableFuture<ExternalAuthToken> exchangeAndSave(
            UserId userId,
            String authorizationCode
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenClient.exchangeAuthorizationCode(authorizationCode)
                .thenCompose(response -> {
                    Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());

                    ExternalAuthToken token = ExternalAuthToken.create(
                            userId,
                            AuthProvider.HH_RU,
                            response.accessToken(),
                            response.refreshToken(),
                            response.tokenType(),
                            response.scope(),
                            expiresAt
                    );

                    return tokenRepository.save(token);
                })
                .whenComplete((token, throwable) -> {
                    if (throwable == null) {
                        log.info("HH OAuth token saved: userId={}, provider={}", userId, AuthProvider.HH_RU);
                    } else {
                        log.warn("Failed to exchange and save HH OAuth token: userId={}", userId, throwable);
                    }
                });
    }

    public CompletableFuture<ExternalAuthToken> refreshAndSave(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenRepository.findByUserIdAndProvider(userId, AuthProvider.HH_RU)
                .thenCompose(optionalToken -> {
                    ExternalAuthToken existingToken = optionalToken.orElseThrow(
                            () -> new HhOAuthTokenRequestException("HH OAuth token not found for user")
                    );

                    return tokenClient.refreshToken(existingToken.refreshToken())
                            .thenCompose(response -> {
                                Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());

                                ExternalAuthToken refreshedToken = existingToken.refresh(
                                        response.accessToken(),
                                        response.refreshToken(),
                                        response.tokenType(),
                                        response.scope(),
                                        expiresAt
                                );

                                return tokenRepository.save(refreshedToken);
                            });
                })
                .whenComplete((token, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "HH OAuth token refreshed and saved: userId={}, provider={}",
                                userId,
                                AuthProvider.HH_RU
                        );
                    } else {
                        log.warn("Failed to refresh HH OAuth token: userId={}", userId, throwable);
                    }
                });
    }
}
