package ru.jobhunter.infrastructure.platform.habr.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.habr.api.dto.HabrCareerCurrentUserResponse;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public final class HabrCareerApiClient {

    private static final Logger log = LoggerFactory.getLogger(HabrCareerApiClient.class);

    private static final String CURRENT_USER_PATH = "/v1/integrations/users/me";

    private final HabrCareerApiRequestExecutor requestExecutor;

    public HabrCareerApiClient(HabrCareerApiRequestExecutor requestExecutor) {
        this.requestExecutor = Objects.requireNonNull(
                requestExecutor,
                "Habr Career API request executor must not be null"
        );
    }

    public CompletableFuture<HabrCareerCurrentUserResponse> getCurrentUser(String accessToken) {
        log.info("Requesting current Habr Career user information");

        return requestExecutor.getAuthorized(
                CURRENT_USER_PATH,
                Map.of(),
                accessToken,
                HabrCareerCurrentUserResponse.class
        );
    }
}