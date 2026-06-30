package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.ReturnAutoResponseQueueItemsToQueuedCommand;
import ru.jobhunter.core.application.dto.ReturnAutoResponseQueueItemsToQueuedResultDto;

import java.util.concurrent.CompletableFuture;

public interface ReturnAutoResponseQueueItemsToQueuedUseCase {

    CompletableFuture<ReturnAutoResponseQueueItemsToQueuedResultDto>
    returnToQueued(
            ReturnAutoResponseQueueItemsToQueuedCommand command
    );
}
