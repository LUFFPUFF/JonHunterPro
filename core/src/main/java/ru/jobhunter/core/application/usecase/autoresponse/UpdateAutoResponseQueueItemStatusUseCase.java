package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.application.dto.UpdateAutoResponseQueueItemStatusCommand;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UpdateAutoResponseQueueItemStatusUseCase {

    CompletableFuture<Optional<AutoResponseQueueItemDto>> updateStatus(
            UpdateAutoResponseQueueItemStatusCommand command
    );
}
