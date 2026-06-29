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
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

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
        Objects.requireNonNull(
                command,
                "Execute auto response command must not be null"
        );

        return queueRepository.findByIdAndUserId(
                        command.queueItemId(),
                        command.userId()
                )
                .thenCompose(optionalItem -> {
                    if (optionalItem.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new AutoResponseQueueItemNotFoundException(
                                        "Auto response queue item was not found"
                                )
                        );
                    }

                    return queueRepository.claimReadyForExecution(
                            command.queueItemId(),
                            command.userId()
                    );
                })
                .thenCompose(optionalClaimedItem -> {
                    AutoResponseQueueItem claimedItem =
                            optionalClaimedItem.orElseThrow(
                                    () -> new AutoResponseQueueItemNotReadyException(
                                            "Auto response queue item is no longer "
                                                    + "READY or is already being executed"
                                    )
                            );

                    AutoResponseExecutionRequest request =
                            toExecutionRequest(claimedItem);

                    Optional<AutoResponseExecutionPort> executionPort =
                            findExecutionPort(claimedItem);

                    CompletableFuture<AutoResponseExecutionResultDto>
                            executionFuture = executionPort
                            .map(port -> port.execute(request)
                                    .exceptionally(throwable ->
                                            toExecutionFailureResult(
                                                    claimedItem,
                                                    throwable
                                            )
                                    ))
                            .orElseGet(() ->
                                    CompletableFuture.completedFuture(
                                            AutoResponseExecutionResultDto
                                                    .notAvailable(
                                                            claimedItem.id(),
                                                            claimedItem.source(),
                                                            claimedItem.externalVacancyId(),
                                                            "Auto response execution "
                                                                    + "is not available "
                                                                    + "for source: "
                                                                    + claimedItem.source()
                                                    )
                                    )
                            );

                    return executionFuture.thenCompose(result ->
                            updateQueueStatusAfterExecution(
                                    command,
                                    result
                            )
                    );
                })
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Auto response execution completed: "
                                        + "userId={}, itemId={}, source={}, "
                                        + "externalVacancyId={}, status={}",
                                command.userId(),
                                result.queueItemId(),
                                result.source(),
                                result.externalVacancyId(),
                                result.status()
                        );
                        return;
                    }

                    log.warn(
                            "Auto response execution failed before completion: "
                                    + "userId={}, itemId={}",
                            command.userId(),
                            command.queueItemId(),
                            throwable
                    );
                });
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

    private AutoResponseExecutionResultDto toExecutionFailureResult(
            AutoResponseQueueItem item,
            Throwable throwable
    ) {
        if (containsLlmProviderUnavailable(throwable)) {
            log.warn(
                    "Auto response execution postponed because LLM provider "
                            + "is temporarily unavailable: itemId={}, "
                            + "source={}, externalVacancyId={}, reason={}",
                    item.id(),
                    item.source(),
                    item.externalVacancyId(),
                    rootMessage(throwable)
            );

            return AutoResponseExecutionResultDto.notAvailable(
                    item.id(),
                    item.source(),
                    item.externalVacancyId(),
                    "LLM временно недоступна. Вакансия оставлена "
                            + "в статусе READY, повторите позже."
            );
        }

        return AutoResponseExecutionResultDto.failed(
                item.id(),
                item.source(),
                item.externalVacancyId(),
                rootMessage(throwable)
        );
    }

    private boolean containsLlmProviderUnavailable(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof LlmProviderUnavailableException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private CompletableFuture<AutoResponseExecutionResultDto> updateQueueStatusAfterExecution(
            ExecuteAutoResponseCommand command,
            AutoResponseExecutionResultDto result
    ) {
        if (result.status()
                == AutoResponseExecutionStatus
                .CANDIDATE_APPROVAL_REQUIRED) {

            return queueRepository.markCandidateApprovalRequired(
                            result.queueItemId(),
                            command.userId(),
                            result.candidateApprovalReason(),
                            result.diagnosticDirectory()
                    )
                    .thenApply(ignored -> result);
        }

        Optional<AutoResponseQueueStatus> targetStatus =
                targetQueueStatus(result.status());

        return targetStatus
                .map(status -> queueRepository.updateStatus(
                        result.queueItemId(),
                        command.userId(),
                        status
                ).thenApply(ignored -> result))
                .orElseGet(() ->
                        CompletableFuture.completedFuture(result)
                );
    }

    private Optional<AutoResponseQueueStatus> targetQueueStatus(AutoResponseExecutionStatus executionStatus) {
        return switch (executionStatus) {
            case SUCCESS, ALREADY_RESPONDED -> Optional.of(AutoResponseQueueStatus.SENT);
            case PARTIAL_SUCCESS -> Optional.of(AutoResponseQueueStatus.PARTIAL_SUCCESS);
            case FAILED -> Optional.of(AutoResponseQueueStatus.FAILED);
            case NOT_AVAILABLE, PREFLIGHT_COMPLETED, QUESTIONNAIRE_REQUIRED, QUESTIONNAIRE_FILLED_REVIEW_REQUIRED ->
                    Optional.of(AutoResponseQueueStatus.READY);
            case CANDIDATE_APPROVAL_REQUIRED -> Optional.empty();
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