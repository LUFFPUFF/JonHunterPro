package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.ExecuteAutoResponseCommand;
import ru.jobhunter.core.application.exception.AutoResponseQueueItemNotFoundException;
import ru.jobhunter.core.application.exception.AutoResponseQueueItemNotReadyException;
import ru.jobhunter.core.application.port.out.AutoResponseExecutionPort;
import ru.jobhunter.core.application.usecase.autoresponse.ExecuteAutoResponseUseCase;
import ru.jobhunter.core.domain.model.AutoResponseExecutionStatus;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public final class AutoResponseExecutionService implements ExecuteAutoResponseUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutoResponseExecutionService.class);

    private final AutoResponseQueueRepository queueRepository;
    private final List<AutoResponseExecutionPort> executionPorts;

    public AutoResponseExecutionService(
            AutoResponseQueueRepository queueRepository,
            List<AutoResponseExecutionPort> executionPorts
    ) {
        this.queueRepository = Objects.requireNonNull(
                queueRepository,
                "Auto response queue repository must not be null"
        );
        this.executionPorts = List.copyOf(Objects.requireNonNull(
                executionPorts,
                "Auto response execution ports must not be null"
        ));
    }

    @Override
    public CompletableFuture<AutoResponseExecutionResultDto> execute(
            ExecuteAutoResponseCommand command
    ) {
        Objects.requireNonNull(command, "Execute auto response command must not be null");

        return queueRepository.findByIdAndUserId(
                        command.queueItemId(),
                        command.userId()
                )
                .thenCompose(optionalItem -> {
                    AutoResponseQueueItem item = optionalItem.orElseThrow(
                            () -> new AutoResponseQueueItemNotFoundException(
                                    "Auto response queue item was not found"
                            )
                    );

                    validateReady(item);

                    AutoResponseExecutionRequest request = toExecutionRequest(item);

                    Optional<AutoResponseExecutionPort> executionPort = findExecutionPort(item);

                    CompletableFuture<AutoResponseExecutionResultDto> executionFuture = executionPort
                            .map(port -> port.execute(request)
                                    .exceptionally(throwable -> AutoResponseExecutionResultDto.failed(
                                            item.id(),
                                            item.source(),
                                            item.externalVacancyId(),
                                            rootMessage(throwable)
                                    )))
                            .orElseGet(() -> CompletableFuture.completedFuture(
                                    AutoResponseExecutionResultDto.notAvailable(
                                            item.id(),
                                            item.source(),
                                            item.externalVacancyId(),
                                            "Auto response execution is not available for source: " + item.source()
                                    )
                            ));

                    return executionFuture.thenCompose(result ->
                            updateQueueStatusAfterExecution(command, result)
                    );
                })
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Auto response execution completed: userId={}, itemId={}, source={}, externalVacancyId={}, status={}",
                                command.userId(),
                                result.queueItemId(),
                                result.source(),
                                result.externalVacancyId(),
                                result.status()
                        );
                    } else {
                        log.warn(
                                "Auto response execution failed before completion: userId={}, itemId={}",
                                command.userId(),
                                command.queueItemId(),
                                throwable
                        );
                    }
                });
    }

    private void validateReady(AutoResponseQueueItem item) {
        if (item.status() != AutoResponseQueueStatus.READY) {
            throw new AutoResponseQueueItemNotReadyException(
                    "Auto response queue item must have READY status before execution"
            );
        }
    }

    private AutoResponseExecutionRequest toExecutionRequest(AutoResponseQueueItem item) {
        return new AutoResponseExecutionRequest(
                item.userId(),
                item.id(),
                item.source(),
                item.externalVacancyId(),
                item.vacancyName(),
                item.vacancyUrl()
        );
    }

    private Optional<AutoResponseExecutionPort> findExecutionPort(
            AutoResponseQueueItem item
    ) {
        return executionPorts.stream()
                .filter(port -> port.supports(item.source()))
                .findFirst();
    }

    private CompletableFuture<AutoResponseExecutionResultDto> updateQueueStatusAfterExecution(
            ExecuteAutoResponseCommand command,
            AutoResponseExecutionResultDto result
    ) {
        AutoResponseQueueStatus queueStatus = toQueueStatus(result.status());

        return queueRepository.updateStatus(
                        result.queueItemId(),
                        command.userId(),
                        queueStatus
                )
                .thenApply(updatedItem -> result);
    }

    private AutoResponseQueueStatus toQueueStatus(AutoResponseExecutionStatus executionStatus) {
        return switch (executionStatus) {
            case SUCCESS -> AutoResponseQueueStatus.SENT;
            case FAILED, NOT_AVAILABLE -> AutoResponseQueueStatus.FAILED;
        };
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}