package ru.jobhunter.ui.controller;

import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;

final class QueueActionButtonsAvailabilityPolicy {

    private QueueActionButtonsAvailabilityPolicy() {
    }

    static boolean shouldDisableAll(
            AutoResponseQueueItemDto selectedItem,
            boolean autoResponseBatchActive,
            boolean queueReadyBatchLoading
    ) {
        return selectedItem == null
                || autoResponseBatchActive
                || queueReadyBatchLoading
                || selectedItem.status() == AutoResponseQueueStatus.IN_PROGRESS;
    }
}