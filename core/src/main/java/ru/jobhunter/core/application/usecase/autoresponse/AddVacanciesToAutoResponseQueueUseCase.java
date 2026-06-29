package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AddVacanciesToAutoResponseQueueCommand;
import ru.jobhunter.core.application.dto.AddVacanciesToAutoResponseQueueResultDto;

import java.util.concurrent.CompletableFuture;

public interface AddVacanciesToAutoResponseQueueUseCase {

    CompletableFuture<AddVacanciesToAutoResponseQueueResultDto> addAllToQueue(
            AddVacanciesToAutoResponseQueueCommand command
    );
}