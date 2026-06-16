package ru.jobhunter.core.application.dto;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record HabrCareerConnectionFlowDto(
        String authorizationUrl,
        String state,
        CompletableFuture<HabrCareerConnectionResultDto> completion
) {

    public HabrCareerConnectionFlowDto {
        authorizationUrl = requireNotBlank(
                authorizationUrl,
                "Habr Career authorization URL must not be blank"
        );

        state = requireNotBlank(
                state,
                "Habr Career OAuth state must not be blank"
        );

        Objects.requireNonNull(
                completion,
                "Habr Career connection completion future must not be null"
        );
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}