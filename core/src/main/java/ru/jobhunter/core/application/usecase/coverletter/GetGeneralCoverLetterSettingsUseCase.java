package ru.jobhunter.core.application.usecase.coverletter;

import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GetGeneralCoverLetterSettingsUseCase {

    CompletableFuture<Optional<GeneralCoverLetterSettingsDto>> findByUserId(
            UserId userId
    );
}
