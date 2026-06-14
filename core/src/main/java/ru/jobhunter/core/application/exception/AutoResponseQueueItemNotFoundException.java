package ru.jobhunter.core.application.exception;

public class AutoResponseQueueItemNotFoundException extends RuntimeException {

    public AutoResponseQueueItemNotFoundException(String message) {
        super(message);
    }
}
