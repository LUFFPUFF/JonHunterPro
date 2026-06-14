package ru.jobhunter.infrastructure.platform.hh.api;

public class HhApiRequestException extends HhApiException {

    public HhApiRequestException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }

    public HhApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}