package ru.jobhunter.infrastructure.platform.hh.autoresponse;

public class HhAutoResponseExecutionException extends RuntimeException {

    public HhAutoResponseExecutionException(String message) {
        super(message);
    }

    public HhAutoResponseExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}