package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetAutoResponseQueueUseCase {

    CompletableFuture<List<AutoResponseQueueItemDto>> getQueue(UserId userId);
}
