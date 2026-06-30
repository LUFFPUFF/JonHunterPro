package ru.jobhunter.infrastructure.platform.habr.autoresponse;

public final class HabrCareerAutoResponseExecutionException
        extends RuntimeException {

    public HabrCareerAutoResponseExecutionException(String message) {
        super(message);
    }

    public HabrCareerAutoResponseExecutionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
