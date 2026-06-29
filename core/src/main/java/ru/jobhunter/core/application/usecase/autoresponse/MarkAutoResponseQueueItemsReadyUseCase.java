package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.MarkAutoResponseQueueItemsReadyCommand;
import ru.jobhunter.core.application.dto.MarkAutoResponseQueueItemsReadyResultDto;

import java.util.concurrent.CompletableFuture;

public interface MarkAutoResponseQueueItemsReadyUseCase {

    CompletableFuture<MarkAutoResponseQueueItemsReadyResultDto> markReady(
            MarkAutoResponseQueueItemsReadyCommand command
    );
}