package ru.jobhunter.infrastructure.platform.habr.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public final class HabrCareerOAuthTokenClient {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final HabrCareerOAuthProperties properties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerOAuthTokenClient(
            HabrCareerOAuthProperties properties,
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career OAuth properties must not be null"
        );
        this.okHttpClient = Objects.requireNonNull(
                okHttpClient,
                "OkHttp client must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Executor service must not be null"
        );
    }

    public CompletableFuture<HabrCareerOAuthTokenResponse> exchangeAuthorizationCode(
            String authorizationCode
    ) {
        String normalizedAuthorizationCode = requireNotBlank(
                authorizationCode,
                "Habr Career authorization code must not be blank"
        );

        validateConfiguration();

        return CompletableFuture.supplyAsync(
                () -> executeAuthorizationCodeExchange(normalizedAuthorizationCode),
                executorService
        );
    }

    private HabrCareerOAuthTokenResponse executeAuthorizationCodeExchange(
            String authorizationCode
    ) {
        FormBody formBody = new FormBody.Builder()
                .add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE)
                .add("client_id", properties.clientId().trim())
                .add("client_secret", properties.clientSecret().trim())
                .add("redirect_uri", properties.redirectUri().trim())
                .add("code", authorizationCode)
                .build();

        Request request = new Request.Builder()
                .url(properties.tokenUrl())
                .header("User-Agent", properties.userAgent())
                .header("Accept", "application/json")
                .post(formBody)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = readResponseBody(response);

            if (!response.isSuccessful()) {
                throw new HabrCareerOAuthTokenRequestException(
                        "Habr Career OAuth token request failed with HTTP status: " + response.code(),
                        response.code(),
                        responseBody
                );
            }

            return objectMapper.readValue(responseBody, HabrCareerOAuthTokenResponse.class);
        } catch (HabrCareerOAuthTokenRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new HabrCareerOAuthTokenRequestException(
                    "Failed to execute Habr Career OAuth token request",
                    exception
            );
        }
    }

    private String readResponseBody(Response response) throws IOException {
        ResponseBody body = response.body();

        if (body == null) {
            return "";
        }

        return body.string();
    }

    private void validateConfiguration() {
        requireNotBlank(properties.tokenUrl(), "Habr Career OAuth token URL is not configured");
        requireNotBlank(properties.clientId(), "Habr Career OAuth client id is not configured");
        requireNotBlank(properties.clientSecret(), "Habr Career OAuth client secret is not configured");
        requireNotBlank(properties.redirectUri(), "Habr Career OAuth redirect URI is not configured");
        requireNotBlank(properties.userAgent(), "Habr Career OAuth User-Agent is not configured");
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new HabrCareerOAuthConfigurationException(message);
        }

        return value.trim();
    }
}