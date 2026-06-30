package ru.jobhunter.infrastructure.service.batch;

import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.domain.model.AutoResponseExecutionStatus;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.Objects;

final class HabrCareerBatchContinuationPolicy {

    private HabrCareerBatchContinuationPolicy() {
    }

    static boolean shouldPauseHabrStream(
            AutoResponseQueueItemDto item,
            AutoResponseExecutionResultDto result
    ) {
        Objects.requireNonNull(item, "Queue item must not be null");
        Objects.requireNonNull(result, "Execution result must not be null");

        if (item.source() != VacancySource.HABR_CAREER) {
            return false;
        }

        return switch (result.status()) {
            case SUCCESS, ALREADY_RESPONDED -> false;
            case PARTIAL_SUCCESS,
                    FAILED,
                    NOT_AVAILABLE,
                    PREFLIGHT_COMPLETED,
                    QUESTIONNAIRE_REQUIRED,
                    QUESTIONNAIRE_FILLED_REVIEW_REQUIRED,
                    CANDIDATE_APPROVAL_REQUIRED -> true;
        };
    }

    static String pauseReason(
            AutoResponseExecutionResultDto result
    ) {
        Objects.requireNonNull(result, "Execution result must not be null");

        return switch (result.status()) {
            case PARTIAL_SUCCESS ->
                    "Habr Career: один отклик создан без подтверждённого "
                            + "сопроводительного письма. Оставшиеся Habr-вакансии "
                            + "не запускались.";
            case CANDIDATE_APPROVAL_REQUIRED ->
                    "Habr Career: для одного отклика требуется проверка "
                            + "кандидатом. Оставшиеся Habr-вакансии не запускались.";
            case FAILED ->
                    "Habr Career: один отклик завершился ошибкой. Оставшиеся "
                            + "Habr-вакансии не запускались.";
            case NOT_AVAILABLE ->
                    "Habr Career временно недоступен. Оставшиеся Habr-вакансии "
                            + "не запускались.";
            case PREFLIGHT_COMPLETED ->
                    "Habr Career запущен в режиме PREFLIGHT. Оставшиеся "
                            + "Habr-вакансии не запускались.";
            case QUESTIONNAIRE_REQUIRED,
                    QUESTIONNAIRE_FILLED_REVIEW_REQUIRED ->
                    "Habr Career вернул состояние, требующее дополнительной "
                            + "проверки. Оставшиеся Habr-вакансии не запускались.";
            case SUCCESS, ALREADY_RESPONDED ->
                    throw new IllegalArgumentException(
                            "Confirmed Habr result must not pause stream"
                    );
        };
    }

    static String pauseReasonAfterUnexpectedFailure() {
        return "Habr Career: выполнение одного отклика завершилось "
                + "непредвиденной ошибкой. Оставшиеся Habr-вакансии "
                + "не запускались.";
    }
}
