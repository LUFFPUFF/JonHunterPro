package ru.jobhunter.core.application.usecase.autoresponse;

import ru.jobhunter.core.application.dto.AddVacancyToAutoResponseQueueCommand;
import ru.jobhunter.core.application.dto.AddVacancyToAutoResponseQueueResultDto;

import java.util.concurrent.CompletableFuture;

public interface AddVacancyToAutoResponseQueueUseCase {

    CompletableFuture<AddVacancyToAutoResponseQueueResultDto> addToQueue(
            AddVacancyToAutoResponseQueueCommand command
    );
}
