package ru.jobhunter.core.application.dto;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record HhConnectionFlowDto(
        String authorizationUrl,
        CompletableFuture<HhConnectionResultDto> completion
) {

    public HhConnectionFlowDto {
        if (authorizationUrl == null || authorizationUrl.isBlank()) {
            throw new IllegalArgumentException("Authorization URL must not be blank");
        }

        authorizationUrl = authorizationUrl.trim();

        Objects.requireNonNull(completion, "OAuth connection completion must not be null");
    }
}
