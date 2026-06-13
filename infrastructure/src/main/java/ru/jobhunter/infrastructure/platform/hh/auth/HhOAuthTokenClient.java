package ru.jobhunter.infrastructure.platform.hh.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Component
public final class HhOAuthTokenClient {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthTokenClient.class);

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HhOAuthProperties properties;
    private final Executor executor;

    public HhOAuthTokenClient(
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            HhOAuthProperties properties,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.properties = Objects.requireNonNull(properties, "HH OAuth properties must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
    }

    public CompletableFuture<HhOAuthTokenResponse> exchangeAuthorizationCode(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new IllegalArgumentException("HH authorization code must not be blank");
        }

        validateConfiguration();

        return CompletableFuture.supplyAsync(() -> executeTokenRequest(authorizationCode.trim()), executor);
    }

    public CompletableFuture<HhOAuthTokenResponse> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("HH refresh token must not be blank");
        }

        validateConfiguration();

        return CompletableFuture.supplyAsync(() -> executeRefreshRequest(refreshToken.trim()), executor);
    }

    private HhOAuthTokenResponse executeTokenRequest(String authorizationCode) {
        FormBody body = new FormBody.Builder()
                .add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE)
                .add("client_id", properties.clientId())
                .add("client_secret", properties.clientSecret())
                .add("code", authorizationCode)
                .add("redirect_uri", properties.redirectUri())
                .build();

        Request request = new Request.Builder()
                .url(properties.tokenUrl())
                .post(body)
                .header("Accept", "application/json")
                .header("User-Agent", properties.userAgent())
                .build();

        log.info("Exchanging HH authorization code for tokens");

        try (var response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();

            if (!response.isSuccessful()) {
                throw new HhOAuthTokenRequestException(
                        "HH token endpoint returned HTTP " + response.code() + ": " + sanitizeErrorBody(responseBody)
                );
            }

            HhOAuthTokenResponse tokenResponse = objectMapper.readValue(
                    responseBody,
                    HhOAuthTokenResponse.class
            );

            log.info(
                    "HH OAuth token response received: tokenType={}, expiresIn={}, scope={}",
                    tokenResponse.tokenType(),
                    tokenResponse.expiresIn(),
                    tokenResponse.scope()
            );

            return tokenResponse;
        } catch (IOException exception) {
            throw new HhOAuthTokenRequestException("Failed to call HH token endpoint", exception);
        }
    }

    private HhOAuthTokenResponse executeRefreshRequest(String refreshToken) {
        FormBody body = new FormBody.Builder()
                .add("grant_type", GRANT_TYPE_REFRESH_TOKEN)
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url(properties.tokenUrl())
                .post(body)
                .header("Accept", "application/json")
                .header("User-Agent", properties.userAgent())
                .build();

        log.info("Refreshing HH OAuth access token");

        try (var response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();

            if (!response.isSuccessful()) {
                throw new HhOAuthTokenRequestException(
                        "HH refresh token endpoint returned HTTP "
                                + response.code()
                                + ": "
                                + sanitizeErrorBody(responseBody)
                );
            }

            HhOAuthTokenResponse tokenResponse = objectMapper.readValue(
                    responseBody,
                    HhOAuthTokenResponse.class
            );

            log.info(
                    "HH OAuth token refreshed: tokenType={}, expiresIn={}, scope={}",
                    tokenResponse.tokenType(),
                    tokenResponse.expiresIn(),
                    tokenResponse.scope()
            );

            return tokenResponse;
        } catch (IOException exception) {
            throw new HhOAuthTokenRequestException("Failed to refresh HH OAuth token", exception);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.tokenUrl())) {
            throw new HhOAuthConfigurationException("HH token URL is not configured");
        }

        if (isBlank(properties.clientId())) {
            throw new HhOAuthConfigurationException("HH client id is not configured");
        }

        if (isBlank(properties.clientSecret())) {
            throw new HhOAuthConfigurationException("HH client secret is not configured");
        }

        if (isBlank(properties.redirectUri())) {
            throw new HhOAuthConfigurationException("HH redirect URI is not configured");
        }

        if (isBlank(properties.userAgent())) {
            throw new HhOAuthConfigurationException("HH User-Agent is not configured");
        }
    }

    private String sanitizeErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty body>";
        }

        return body.length() > 512 ? body.substring(0, 512) + "..." : body;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}