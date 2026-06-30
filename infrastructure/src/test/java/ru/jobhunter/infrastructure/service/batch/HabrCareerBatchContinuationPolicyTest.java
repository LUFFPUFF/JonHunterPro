package ru.jobhunter.infrastructure.service.batch;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabrCareerBatchContinuationPolicyTest {

    @Test
    void shouldContinueHabrStreamAfterConfirmedSentResponse() {
        AutoResponseQueueItemDto item = readyHabrItem("1001");
        AutoResponseExecutionResultDto result =
                AutoResponseExecutionResultDto.success(
                        AutoResponseQueueItemId.of(item.id()),
                        VacancySource.HABR_CAREER,
                        item.externalVacancyId(),
                        "Cover letter verified"
                );

        assertFalse(
                HabrCareerBatchContinuationPolicy.shouldPauseHabrStream(
                        item,
                        result
                )
        );
    }

    @Test
    void shouldPauseHabrStreamAfterPartialSuccess() {
        AutoResponseQueueItemDto item = readyHabrItem("1002");
        AutoResponseExecutionResultDto result =
                AutoResponseExecutionResultDto.partialSuccess(
                        AutoResponseQueueItemId.of(item.id()),
                        VacancySource.HABR_CAREER,
                        item.externalVacancyId(),
                        "Response sent without verified letter"
                );

        assertTrue(
                HabrCareerBatchContinuationPolicy.shouldPauseHabrStream(
                        item,
                        result
                )
        );
    }

    @Test
    void shouldNotPauseForHhResult() {
        AutoResponseQueueItemDto item = readyHhItem("1003");
        AutoResponseExecutionResultDto result =
                AutoResponseExecutionResultDto.failed(
                        AutoResponseQueueItemId.of(item.id()),
                        VacancySource.HH_RU,
                        item.externalVacancyId(),
                        "HH failed"
                );

        assertFalse(
                HabrCareerBatchContinuationPolicy.shouldPauseHabrStream(
                        item,
                        result
                )
        );
    }

    private AutoResponseQueueItemDto readyHabrItem(String externalVacancyId) {
        return readyItem(VacancySource.HABR_CAREER, externalVacancyId);
    }

    private AutoResponseQueueItemDto readyHhItem(String externalVacancyId) {
        return readyItem(VacancySource.HH_RU, externalVacancyId);
    }

    private AutoResponseQueueItemDto readyItem(
            VacancySource source,
            String externalVacancyId
    ) {
        Instant now = Instant.parse("2026-06-30T17:00:00Z");

        return new AutoResponseQueueItemDto(
                UUID.randomUUID(),
                source,
                externalVacancyId,
                "Java Developer",
                "Example Company",
                "Москва",
                "https://example.test/vacancies/" + externalVacancyId,
                AutoResponseQueueStatus.READY,
                now,
                now
        );
    }
}
