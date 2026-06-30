package ru.jobhunter.infrastructure.platform.habr.browser;

public final class HabrCareerResponseFormProbeException
        extends RuntimeException {

    public HabrCareerResponseFormProbeException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
