package ru.jobhunter.ui.controller;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueueActionButtonsAvailabilityPolicyTest {

    @Test
    void shouldDisableActionsWhenSelectionIsCleared() {
        boolean disabled = QueueActionButtonsAvailabilityPolicy.shouldDisableAll(
                null,
                false,
                false
        );

        assertThat(disabled).isTrue();
    }

    @Test
    void shouldDisableActionsWhenBatchIsActive() {
        boolean disabled = QueueActionButtonsAvailabilityPolicy.shouldDisableAll(
                queueItem(AutoResponseQueueStatus.READY),
                true,
                false
        );

        assertThat(disabled).isTrue();
    }

    @Test
    void shouldDisableActionsWhenReadyBatchProcessingIsActive() {
        boolean disabled = QueueActionButtonsAvailabilityPolicy.shouldDisableAll(
                queueItem(AutoResponseQueueStatus.READY),
                false,
                true
        );

        assertThat(disabled).isTrue();
    }

    @Test
    void shouldDisableActionsForItemInProgress() {
        boolean disabled = QueueActionButtonsAvailabilityPolicy.shouldDisableAll(
                queueItem(AutoResponseQueueStatus.IN_PROGRESS),
                false,
                false
        );

        assertThat(disabled).isTrue();
    }

    @Test
    void shouldKeepActionsAvailableForSelectedReadyItem() {
        boolean disabled = QueueActionButtonsAvailabilityPolicy.shouldDisableAll(
                queueItem(AutoResponseQueueStatus.READY),
                false,
                false
        );

        assertThat(disabled).isFalse();
    }

    private AutoResponseQueueItemDto queueItem(
            AutoResponseQueueStatus status
    ) {
        Instant now = Instant.parse("2026-06-28T09:00:00Z");

        return new AutoResponseQueueItemDto(
                UUID.fromString("c0ee1f09-98bd-44ca-86ba-01a87fc8f572"),
                VacancySource.HH_RU,
                "123456789",
                "Java Developer",
                "Example employer",
                "Voronezh",
                "https://hh.ru/vacancy/123456789",
                status,
                null,
                null,
                now,
                now
        );
    }
}