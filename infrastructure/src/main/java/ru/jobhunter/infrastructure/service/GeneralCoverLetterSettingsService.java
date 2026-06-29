package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.application.dto.SaveGeneralCoverLetterSettingsCommand;
import ru.jobhunter.core.application.usecase.coverletter.GetGeneralCoverLetterSettingsUseCase;
import ru.jobhunter.core.application.usecase.coverletter.SaveGeneralCoverLetterSettingsUseCase;
import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.repository.GeneralCoverLetterSettingsRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class GeneralCoverLetterSettingsService implements
        GetGeneralCoverLetterSettingsUseCase,
        SaveGeneralCoverLetterSettingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            GeneralCoverLetterSettingsService.class
    );

    private final GeneralCoverLetterSettingsRepository repository;

    public GeneralCoverLetterSettingsService(
            GeneralCoverLetterSettingsRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "General cover letter settings repository must not be null"
        );
    }

    @Override
    public CompletableFuture<Optional<GeneralCoverLetterSettingsDto>> findByUserId(
            ru.jobhunter.core.domain.model.UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return repository.findByUserId(userId)
                .thenApply(optionalSettings -> optionalSettings.map(
                        GeneralCoverLetterSettingsDto::from
                ));
    }

    @Override
    public CompletableFuture<GeneralCoverLetterSettingsDto> save(
            SaveGeneralCoverLetterSettingsCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Save general cover letter settings command must not be null"
        );

        return repository.findByUserId(command.userId())
                .thenCompose(optionalSettings -> {
                    GeneralCoverLetterSettings settingsToSave =
                            optionalSettings
                                    .map(existing -> existing.update(
                                            command.content(),
                                            command.useWhenLlmUnavailable(),
                                            command.sourceFileName()
                                    ))
                                    .orElseGet(() ->
                                            GeneralCoverLetterSettings.create(
                                                    command.userId(),
                                                    command.content(),
                                                    command.useWhenLlmUnavailable(),
                                                    command.sourceFileName()
                                            )
                                    );

                    return repository.save(settingsToSave);
                })
                .thenApply(GeneralCoverLetterSettingsDto::from)
                .whenComplete((settings, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "General cover letter settings saved: userId={}, fallbackEnabled={}",
                                settings.userId(),
                                settings.useWhenLlmUnavailable()
                        );
                        return;
                    }

                    log.warn(
                            "General cover letter settings save failed: userId={}",
                            command.userId(),
                            throwable
                    );
                });
    }
}