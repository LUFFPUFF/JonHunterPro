package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.ExecuteAutoResponseCommand;

import java.util.concurrent.CompletableFuture;

public interface ExecuteAutoResponseUseCase {

    CompletableFuture<AutoResponseExecutionResultDto> execute(
            ExecuteAutoResponseCommand command
    );
}
