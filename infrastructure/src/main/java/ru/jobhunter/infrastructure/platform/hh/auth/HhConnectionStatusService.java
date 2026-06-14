package ru.jobhunter.infrastructure.platform.hh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhConnectionStatusDto;
import ru.jobhunter.core.application.usecase.integration.GetHhConnectionStatusUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhConnectionStatusService implements GetHhConnectionStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(HhConnectionStatusService.class);

    private final ExternalAuthTokenRepository tokenRepository;

    public HhConnectionStatusService(ExternalAuthTokenRepository tokenRepository) {
        this.tokenRepository = Objects.requireNonNull(
                tokenRepository,
                "External auth token repository must not be null"
        );
    }

    @Override
    public CompletableFuture<HhConnectionStatusDto> getStatus(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenRepository.findByUserIdAndProvider(userId, AuthProvider.HH_RU)
                .thenApply(optionalToken -> {
                    Instant checkedAt = Instant.now();

                    return optionalToken
                            .map(token -> toStatus(token, checkedAt))
                            .orElseGet(() -> HhConnectionStatusDto.disconnected(checkedAt));
                })
                .whenComplete((status, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "HH.ru connection status checked: userId={}, status={}",
                                userId,
                                status.status()
                        );
                    } else {
                        log.warn("Failed to check HH.ru connection status: userId={}", userId, throwable);
                    }
                });
    }

    private HhConnectionStatusDto toStatus(ExternalAuthToken token, Instant checkedAt) {
        Instant expiresAt = token.expiresAt();

        if (expiresAt.isAfter(checkedAt)) {
            return HhConnectionStatusDto.connected(expiresAt, checkedAt);
        }

        return HhConnectionStatusDto.expired(expiresAt, checkedAt);
    }

}
