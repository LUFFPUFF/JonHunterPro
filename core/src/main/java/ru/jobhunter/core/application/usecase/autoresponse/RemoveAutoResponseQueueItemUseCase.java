package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.RemoveAutoResponseQueueItemCommand;

import java.util.concurrent.CompletableFuture;

public interface RemoveAutoResponseQueueItemUseCase {

    CompletableFuture<Boolean> removeFromQueue(RemoveAutoResponseQueueItemCommand command);
}
