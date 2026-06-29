package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.application.usecase.coverletter.CoverLetterQualityValidator;
import ru.jobhunter.core.application.usecase.coverletter.GeneratedCoverLetterQualityException;
import ru.jobhunter.core.application.usecase.coverletter.GetGeneralCoverLetterSettingsUseCase;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class GeneralCoverLetterFallbackResolver {

    private static final Logger log = LoggerFactory.getLogger(
            GeneralCoverLetterFallbackResolver.class
    );

    private final GetGeneralCoverLetterSettingsUseCase
            getGeneralCoverLetterSettingsUseCase;

    public GeneralCoverLetterFallbackResolver(
            GetGeneralCoverLetterSettingsUseCase
                    getGeneralCoverLetterSettingsUseCase
    ) {
        this.getGeneralCoverLetterSettingsUseCase =
                Objects.requireNonNull(
                        getGeneralCoverLetterSettingsUseCase,
                        "Get general cover letter settings use case "
                                + "must not be null"
                );
    }

    public CompletableFuture<Optional<String>> resolve(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return getGeneralCoverLetterSettingsUseCase.findByUserId(userId)
                .thenApply(optionalSettings -> optionalSettings.flatMap(
                        this::validateEnabledFallback
                ));
    }

    private Optional<String> validateEnabledFallback(
            GeneralCoverLetterSettingsDto settings
    ) {
        if (!settings.useWhenLlmUnavailable()) {
            log.debug(
                    "General cover letter fallback is disabled: userId={}",
                    settings.userId()
            );

            return Optional.empty();
        }

        try {
            String normalized = CoverLetterQualityValidator
                    .validateAndNormalize(settings.content());

            log.debug(
                    "General cover letter fallback is available: "
                            + "userId={}, sourceFileName={}, "
                            + "coverLetterLength={}",
                    settings.userId(),
                    settings.sourceFileName(),
                    normalized.length()
            );

            return Optional.of(normalized);
        } catch (GeneratedCoverLetterQualityException exception) {
            int contentLength = settings.content() == null
                    ? 0
                    : settings.content().strip().length();

            log.warn(
                    "General cover letter fallback was ignored because it "
                            + "did not pass quality gate: userId={}, "
                            + "sourceFileName={}, coverLetterLength={}, "
                            + "reason={}",
                    settings.userId(),
                    settings.sourceFileName(),
                    contentLength,
                    exception.getMessage()
            );

            return Optional.empty();
        }
    }
}