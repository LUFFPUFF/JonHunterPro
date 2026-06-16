package ru.jobhunter.infrastructure.platform.habr.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public final class HabrCareerOAuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(HabrCareerOAuthTokenService.class);

    private static final String TOKEN_TYPE_BEARER = "bearer";

    private final HabrCareerOAuthTokenClient tokenClient;
    private final ExternalAuthTokenRepository tokenRepository;

    public HabrCareerOAuthTokenService(
            HabrCareerOAuthTokenClient tokenClient,
            ExternalAuthTokenRepository tokenRepository
    ) {
        this.tokenClient = Objects.requireNonNull(
                tokenClient,
                "Habr Career OAuth token client must not be null"
        );
        this.tokenRepository = Objects.requireNonNull(
                tokenRepository,
                "External auth token repository must not be null"
        );
    }

    public CompletableFuture<ExternalAuthToken> exchangeAndSave(
            UserId userId,
            String authorizationCode
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenClient.exchangeAuthorizationCode(authorizationCode)
                .thenCompose(response -> {
                    ExternalAuthToken token = ExternalAuthToken.createPermanent(
                            userId,
                            AuthProvider.HABR_CAREER,
                            response.accessToken(),
                            TOKEN_TYPE_BEARER,
                            null
                    );

                    return tokenRepository.save(token);
                })
                .whenComplete((token, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Habr Career OAuth token saved: userId={}, provider={}",
                                userId,
                                AuthProvider.HABR_CAREER
                        );
                    } else {
                        log.warn(
                                "Failed to exchange and save Habr Career OAuth token: userId={}",
                                userId,
                                throwable
                        );
                    }
                });
    }
}
