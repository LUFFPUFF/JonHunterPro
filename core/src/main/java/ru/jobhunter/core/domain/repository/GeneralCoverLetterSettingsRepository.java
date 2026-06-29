package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GeneralCoverLetterSettingsRepository {

    CompletableFuture<GeneralCoverLetterSettings> save(
            GeneralCoverLetterSettings settings
    );

    CompletableFuture<Optional<GeneralCoverLetterSettings>> findByUserId(
            UserId userId
    );
}