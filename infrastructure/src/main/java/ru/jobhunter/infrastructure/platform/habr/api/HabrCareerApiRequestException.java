package ru.jobhunter.infrastructure.platform.habr.api;

public class HabrCareerApiRequestException extends HabrCareerApiException {

    public HabrCareerApiRequestException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }

    public HabrCareerApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
