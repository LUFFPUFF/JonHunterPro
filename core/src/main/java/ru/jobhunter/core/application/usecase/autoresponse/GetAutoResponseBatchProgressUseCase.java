package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AutoResponseBatchProgressDto;

import java.util.Optional;
import java.util.UUID;

public interface GetAutoResponseBatchProgressUseCase {

    Optional<AutoResponseBatchProgressDto> getProgress(
            UUID batchId
    );
}
