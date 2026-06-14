package ru.jobhunter.core.application.exception;

public class AutoResponseQueueItemNotReadyException extends RuntimeException {

    public AutoResponseQueueItemNotReadyException(String message) {
        super(message);
    }
}
