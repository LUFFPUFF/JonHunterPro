package ru.jobhunter.infrastructure.platform.hh.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhResumeDto;
import ru.jobhunter.core.application.exception.HhAccountNotConnectedException;
import ru.jobhunter.core.application.usecase.integration.GetHhResumesUseCase;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhMineResumesResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhResumeItemResponse;
import ru.jobhunter.infrastructure.platform.hh.auth.HhOAuthTokenService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhResumeService implements GetHhResumesUseCase {

    private static final Logger log = LoggerFactory.getLogger(HhResumeService.class);

    private static final Duration TOKEN_REFRESH_SKEW = Duration.ofMinutes(1);

    private final ExternalAuthTokenRepository tokenRepository;
    private final HhOAuthTokenService tokenService;
    private final HhApiClient apiClient;

    public HhResumeService(
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
    public CompletableFuture<List<HhResumeDto>> getResumes(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return tokenRepository.findByUserIdAndProvider(userId, AuthProvider.HH_RU)
                .thenCompose(optionalToken -> {
                    ExternalAuthToken token = optionalToken.orElseThrow(
                            () -> new HhAccountNotConnectedException("HH.ru account is not connected")
                    );

                    if (shouldRefresh(token)) {
                        log.info("HH.ru access token is expired or close to expiration before loading resumes: userId={}", userId);
                        return tokenService.refreshAndSave(userId);
                    }

                    return CompletableFuture.completedFuture(token);
                })
                .thenCompose(token -> apiClient.getMyResumes(token.accessToken()))
                .thenApply(this::toDtoList)
                .whenComplete((resumes, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Current HH.ru user resumes loaded successfully: userId={}, count={}",
                                userId,
                                resumes.size()
                        );
                    } else {
                        log.warn("Failed to load current HH.ru user resumes: userId={}", userId, throwable);
                    }
                });
    }

    private boolean shouldRefresh(ExternalAuthToken token) {
        if (token.expiresAt() == null) {
            return false;
        }

        Instant refreshThreshold = Instant.now().plus(TOKEN_REFRESH_SKEW);

        return !token.expiresAt().isAfter(refreshThreshold);
    }

    private List<HhResumeDto> toDtoList(HhMineResumesResponse response) {
        if (response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(this::toDto)
                .toList();
    }

    private HhResumeDto toDto(HhResumeItemResponse resume) {
        String statusName = resume.status() == null
                ? null
                : resume.status().name();

        return new HhResumeDto(
                resume.id(),
                resume.title(),
                resume.url(),
                resume.alternateUrl(),
                statusName,
                Instant.now()
        );
    }
}