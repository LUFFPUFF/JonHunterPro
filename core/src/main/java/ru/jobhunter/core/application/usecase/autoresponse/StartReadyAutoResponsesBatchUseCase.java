package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchCommand;
import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchResultDto;

import java.util.concurrent.CompletableFuture;

public interface StartReadyAutoResponsesBatchUseCase {

    CompletableFuture<StartReadyAutoResponsesBatchResultDto> start(
            StartReadyAutoResponsesBatchCommand command
    );
}