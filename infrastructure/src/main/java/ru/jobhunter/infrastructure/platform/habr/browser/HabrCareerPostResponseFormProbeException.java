package ru.jobhunter.infrastructure.platform.habr.browser;

public final class HabrCareerPostResponseFormProbeException
        extends RuntimeException {

    public HabrCareerPostResponseFormProbeException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
