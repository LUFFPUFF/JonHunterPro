package ru.jobhunter.core.domain.exception;

public final class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
