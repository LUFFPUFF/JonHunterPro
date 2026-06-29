package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.ExecuteAutoResponseCommand;
import ru.jobhunter.core.application.port.out.AutoResponseExecutionPort;
import ru.jobhunter.core.domain.model.AutoResponseExecutionStatus;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;
import ru.jobhunter.infrastructure.llm.openrouter.OpenRouterRateLimitException;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoResponseExecutionServiceTest {

    @Test
    void shouldKeepQueueItemReadyWhenLlmProviderIsUnavailable() {
        AutoResponseQueueItem item = readyQueueItem();

        InMemoryQueueRepository queueRepository =
                new InMemoryQueueRepository(item);

        StubExecutionPort executionPort = new StubExecutionPort(
                VacancySource.HH_RU,
                CompletableFuture.failedFuture(
                        new LlmProviderUnavailableException(
                                "openrouter",
                                LlmFailureCategory.OPENROUTER_EMPTY_CONTENT,
                                "OpenRouter response content is empty"
                        )
                )
        );

        AutoResponseExecutionService service =
                new AutoResponseExecutionService(
                        queueRepository,
                        List.of(executionPort)
                );

        AutoResponseExecutionResultDto result = service.execute(
                commandFor(item)
        ).join();

        assertEquals(
                AutoResponseExecutionStatus.NOT_AVAILABLE,
                result.status()
        );
        assertEquals(1, executionPort.executeCalls());
        assertEquals(
                List.of(AutoResponseQueueStatus.READY),
                queueRepository.statusUpdates()
        );
        assertEquals(
                AutoResponseQueueStatus.READY,
                queueRepository.currentItem().status()
        );
    }

    @Test
    void shouldKeepQueueItemReadyWhenOpenRouterRateLimitIsReached() {
        AutoResponseQueueItem item = readyQueueItem();

        InMemoryQueueRepository queueRepository =
                new InMemoryQueueRepository(item);

        StubExecutionPort executionPort = new StubExecutionPort(
                VacancySource.HH_RU,
                CompletableFuture.failedFuture(
                        new OpenRouterRateLimitException(
                                "qwen/qwen3-next-80b-a3b-instruct:free",
                                "Rate limit exceeded: free-models-per-day",
                                Duration.ofMinutes(15)
                        )
                )
        );

        AutoResponseExecutionService service =
                new AutoResponseExecutionService(
                        queueRepository,
                        List.of(executionPort)
                );

        AutoResponseExecutionResultDto result = service.execute(
                commandFor(item)
        ).join();

        assertEquals(
                AutoResponseExecutionStatus.NOT_AVAILABLE,
                result.status()
        );
        assertEquals(1, executionPort.executeCalls());
        assertEquals(
                List.of(AutoResponseQueueStatus.READY),
                queueRepository.statusUpdates()
        );
        assertEquals(
                AutoResponseQueueStatus.READY,
                queueRepository.currentItem().status()
        );
    }

    @Test
    void shouldKeepQueueItemReadyWhenCoverLetterQualityGateRejectsText() {
        AutoResponseQueueItem item = readyQueueItem();

        InMemoryQueueRepository queueRepository =
                new InMemoryQueueRepository(item);

        StubExecutionPort executionPort = new StubExecutionPort(
                VacancySource.HH_RU,
                CompletableFuture.failedFuture(
                        new LlmProviderUnavailableException(
                                "cover-letter-quality",
                                LlmFailureCategory.INVALID_MODEL_OUTPUT,
                                "Generated cover letter was rejected by "
                                        + "quality gate: sentences=1, minimum=2"
                        )
                )
        );

        AutoResponseExecutionService service =
                new AutoResponseExecutionService(
                        queueRepository,
                        List.of(executionPort)
                );

        AutoResponseExecutionResultDto result = service.execute(
                commandFor(item)
        ).join();

        assertEquals(
                AutoResponseExecutionStatus.NOT_AVAILABLE,
                result.status()
        );
        assertEquals(1, executionPort.executeCalls());
        assertEquals(
                List.of(AutoResponseQueueStatus.READY),
                queueRepository.statusUpdates()
        );
        assertEquals(
                AutoResponseQueueStatus.READY,
                queueRepository.currentItem().status()
        );
    }

    @Test
    void shouldStorePartialSuccessSeparatelyFromSentStatus() {
        AutoResponseQueueItem item = readyQueueItem();
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(item);
        StubExecutionPort executionPort = new StubExecutionPort(VacancySource.HH_RU, CompletableFuture.completedFuture(AutoResponseExecutionResultDto.partialSuccess(item.id(), item.source(), item.externalVacancyId(), "HH accepted the resume, but cover letter " + "was not confirmed")));
        AutoResponseExecutionService service = new AutoResponseExecutionService(queueRepository, List.of(executionPort));
        AutoResponseExecutionResultDto result = service.execute(commandFor(item)).join();
        assertEquals(AutoResponseExecutionStatus.PARTIAL_SUCCESS, result.status());
        assertEquals(List.of(AutoResponseQueueStatus.PARTIAL_SUCCESS), queueRepository.statusUpdates());
        assertEquals(AutoResponseQueueStatus.PARTIAL_SUCCESS, queueRepository.currentItem().status());
    }

    @Test
    void shouldMarkQueueItemFailedForNonRetryableExecutionFailure() {
        AutoResponseQueueItem item = readyQueueItem();

        InMemoryQueueRepository queueRepository =
                new InMemoryQueueRepository(item);

        StubExecutionPort executionPort = new StubExecutionPort(
                VacancySource.HH_RU,
                CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "HH browser automation failed"
                        )
                )
        );

        AutoResponseExecutionService service =
                new AutoResponseExecutionService(
                        queueRepository,
                        List.of(executionPort)
                );

        AutoResponseExecutionResultDto result = service.execute(
                commandFor(item)
        ).join();

        assertEquals(
                AutoResponseExecutionStatus.FAILED,
                result.status()
        );
        assertEquals(1, executionPort.executeCalls());
        assertEquals(
                List.of(AutoResponseQueueStatus.FAILED),
                queueRepository.statusUpdates()
        );
        assertEquals(
                AutoResponseQueueStatus.FAILED,
                queueRepository.currentItem().status()
        );
    }

    private ExecuteAutoResponseCommand commandFor(
            AutoResponseQueueItem item
    ) {
        return new ExecuteAutoResponseCommand(
                item.userId(),
                item.id()
        );
    }

    private AutoResponseQueueItem readyQueueItem() {
        Instant now = Instant.parse("2026-06-22T12:00:00Z");

        return new AutoResponseQueueItem(
                AutoResponseQueueItemId.newId(),
                UserId.newId(),
                VacancySource.HH_RU,
                "vacancy-123",
                "Java Developer",
                "Example Company",
                "Voronezh",
                "https://voronezh.hh.ru/vacancy/123",
                AutoResponseQueueStatus.READY,
                now,
                now
        );
    }

    private static final class StubExecutionPort
            implements AutoResponseExecutionPort {

        private final VacancySource supportedSource;
        private final CompletableFuture<AutoResponseExecutionResultDto>
                executionFuture;

        private int executeCalls;

        private StubExecutionPort(
                VacancySource supportedSource,
                CompletableFuture<AutoResponseExecutionResultDto>
                        executionFuture
        ) {
            this.supportedSource = supportedSource;
            this.executionFuture = executionFuture;
        }

        @Override
        public boolean supports(VacancySource source) {
            return supportedSource == source;
        }

        @Override
        public CompletableFuture<AutoResponseExecutionResultDto> execute(
                AutoResponseExecutionRequest request
        ) {
            executeCalls++;

            return executionFuture;
        }

        private int executeCalls() {
            return executeCalls;
        }
    }

    private static final class InMemoryQueueRepository
            implements AutoResponseQueueRepository {

        private AutoResponseQueueItem currentItem;
        private final List<AutoResponseQueueStatus> statusUpdates =
                new ArrayList<>();

        private InMemoryQueueRepository(AutoResponseQueueItem item) {
            this.currentItem = item;
        }

        @Override
        public CompletableFuture<AutoResponseQueueItem> save(
                AutoResponseQueueItem item
        ) {
            currentItem = item;

            return CompletableFuture.completedFuture(item);
        }

        @Override
        public CompletableFuture<Optional<AutoResponseQueueItem>>
        findByUserIdAndSourceAndExternalVacancyId(
                UserId userId,
                VacancySource source,
                String externalVacancyId
        ) {
            boolean matches = currentItem.userId().equals(userId)
                    && currentItem.source() == source
                    && currentItem.externalVacancyId()
                    .equals(externalVacancyId);

            return CompletableFuture.completedFuture(
                    matches
                            ? Optional.of(currentItem)
                            : Optional.empty()
            );
        }

        @Override
        public CompletableFuture<List<AutoResponseQueueItem>>
        findByUserIdOrderByCreatedAtDesc(UserId userId) {
            return CompletableFuture.completedFuture(
                    currentItem.userId().equals(userId)
                            ? List.of(currentItem)
                            : List.of()
            );
        }

        @Override
        public CompletableFuture<List<AutoResponseQueueItem>>
        findByUserIdAndStatusOrderByCreatedAtDesc(
                UserId userId,
                AutoResponseQueueStatus status
        ) {
            boolean matches = currentItem.userId().equals(userId)
                    && currentItem.status() == status;

            return CompletableFuture.completedFuture(
                    matches
                            ? List.of(currentItem)
                            : List.of()
            );
        }

        @Override
        public CompletableFuture<Optional<AutoResponseQueueItem>>
        findByIdAndUserId(
                AutoResponseQueueItemId itemId,
                UserId userId
        ) {
            boolean matches = currentItem.id().equals(itemId)
                    && currentItem.userId().equals(userId);

            return CompletableFuture.completedFuture(
                    matches
                            ? Optional.of(currentItem)
                            : Optional.empty()
            );
        }

        @Override
        public CompletableFuture<Boolean> deleteByIdAndUserId(
                AutoResponseQueueItemId itemId,
                UserId userId
        ) {
            boolean matches = currentItem.id().equals(itemId)
                    && currentItem.userId().equals(userId);

            return CompletableFuture.completedFuture(matches);
        }

        @Override
        public CompletableFuture<Optional<AutoResponseQueueItem>>
        updateStatus(
                AutoResponseQueueItemId itemId,
                UserId userId,
                AutoResponseQueueStatus status
        ) {
            boolean matches = currentItem.id().equals(itemId)
                    && currentItem.userId().equals(userId);

            if (!matches) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            statusUpdates.add(status);
            currentItem = currentItem.withStatus(status);

            return CompletableFuture.completedFuture(
                    Optional.of(currentItem)
            );
        }

        @Override
        public CompletableFuture<Optional<AutoResponseQueueItem>>
        markCandidateApprovalRequired(
                AutoResponseQueueItemId itemId,
                UserId userId,
                String approvalReason,
                String diagnosticDirectory
        ) {
            boolean matches = currentItem.id().equals(itemId)
                    && currentItem.userId().equals(userId);

            if (!matches) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            currentItem = currentItem.withCandidateApprovalRequired(
                    approvalReason,
                    diagnosticDirectory
            );

            return CompletableFuture.completedFuture(
                    Optional.of(currentItem)
            );
        }

        @Override
        public CompletableFuture<Optional<AutoResponseQueueItem>>
        claimReadyForExecution(
                AutoResponseQueueItemId itemId,
                UserId userId
        ) {
            boolean matches = currentItem.id().equals(itemId)
                    && currentItem.userId().equals(userId)
                    && currentItem.status()
                    == AutoResponseQueueStatus.READY;

            if (!matches) {
                return CompletableFuture.completedFuture(
                        Optional.empty()
                );
            }

            currentItem = currentItem.withStatus(
                    AutoResponseQueueStatus.IN_PROGRESS
            );

            return CompletableFuture.completedFuture(
                    Optional.of(currentItem)
            );
        }

        private AutoResponseQueueItem currentItem() {
            return currentItem;
        }

        private List<AutoResponseQueueStatus> statusUpdates() {
            return List.copyOf(statusUpdates);
        }
    }
}
