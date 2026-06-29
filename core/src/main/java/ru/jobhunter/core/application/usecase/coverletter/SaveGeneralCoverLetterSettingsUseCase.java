package ru.jobhunter.core.application.usecase.coverletter;

import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.application.dto.SaveGeneralCoverLetterSettingsCommand;

import java.util.concurrent.CompletableFuture;

public interface SaveGeneralCoverLetterSettingsUseCase {

    CompletableFuture<GeneralCoverLetterSettingsDto> save(
            SaveGeneralCoverLetterSettingsCommand command
    );
}