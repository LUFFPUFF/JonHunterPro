package ru.jobhunter.core.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AutoResponseQueueItemTest {
    @Test
    void shouldNotAllowPartialSuccessItemToReturnToReady() {
        Instant now = Instant.parse("2026-06-28T12:00:00Z");
        AutoResponseQueueItem item = new AutoResponseQueueItem(AutoResponseQueueItemId.newId(), UserId.of(UUID.randomUUID()), VacancySource.HH_RU, "123456", "Java Developer", "Example Company", "Москва", "https://hh.ru/vacancy/123456", AutoResponseQueueStatus.PARTIAL_SUCCESS, now, now);
        assertThrows(IllegalStateException.class, () -> item.withStatus(AutoResponseQueueStatus.READY));
    }
}