package ru.jobhunter.core.application.dto;

import java.util.Objects;
import java.util.UUID;

public record StartReadyAutoResponsesBatchResultDto(
        AutoResponseBatchStartStatus status,
        UUID batchId,
        int plannedCount,
        String message
) {

    public StartReadyAutoResponsesBatchResultDto {
        Objects.requireNonNull(
                status,
                "Batch start status must not be null"
        );

        if (plannedCount < 0) {
            throw new IllegalArgumentException(
                    "Planned count must not be negative"
            );
        }

        message = normalize(
                message,
                "Неизвестный результат запуска batch-обработки."
        );

        if (status == AutoResponseBatchStartStatus.NO_READY_ITEMS) {
            if (batchId != null || plannedCount != 0) {
                throw new IllegalArgumentException(
                        "NO_READY_ITEMS must not contain batch id "
                                + "or planned items"
                );
            }
        } else {
            if (batchId == null) {
                throw new IllegalArgumentException(
                        "Batch id must not be null"
                );
            }

            if (plannedCount < 1) {
                throw new IllegalArgumentException(
                        "Started batch must contain at least one item"
                );
            }
        }
    }

    public static StartReadyAutoResponsesBatchResultDto started(
            UUID batchId,
            int plannedCount
    ) {
        return new StartReadyAutoResponsesBatchResultDto(
                AutoResponseBatchStartStatus.STARTED,
                batchId,
                plannedCount,
                "Массовый запуск автооткликов начат."
        );
    }

    public static StartReadyAutoResponsesBatchResultDto alreadyRunning(
            UUID batchId,
            int plannedCount
    ) {
        return new StartReadyAutoResponsesBatchResultDto(
                AutoResponseBatchStartStatus.ALREADY_RUNNING,
                batchId,
                plannedCount,
                "Автоотклики уже выполняются."
        );
    }

    public static StartReadyAutoResponsesBatchResultDto noReadyItems() {
        return new StartReadyAutoResponsesBatchResultDto(
                AutoResponseBatchStartStatus.NO_READY_ITEMS,
                null,
                0,
                "Нет вакансий со статусом READY."
        );
    }

    public static StartReadyAutoResponsesBatchResultDto failedToStart(
            UUID batchId,
            int plannedCount,
            String message
    ) {
        return new StartReadyAutoResponsesBatchResultDto(
                AutoResponseBatchStartStatus.FAILED_TO_START,
                batchId,
                plannedCount,
                message
        );
    }

    private static String normalize(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : value.strip();
    }
}