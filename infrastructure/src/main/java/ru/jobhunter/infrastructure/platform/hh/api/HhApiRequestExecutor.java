package ru.jobhunter.infrastructure.platform.hh.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class HhApiRequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(HhApiRequestExecutor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HhApiProperties properties;
    private final ExecutorService executorService;


    public HhApiRequestExecutor(
            @Qualifier("okHttpClient") OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            HhApiProperties properties,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.httpClient = Objects.requireNonNull(okHttpClient, "OkHttp client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.properties = Objects.requireNonNull(properties, "HH API properties must not be null");
        this.executorService = Objects.requireNonNull(executorService, "Executor service must not be null");
    }

    public <T> CompletableFuture<T> postAuthorizedJson(
            String path,
            Object requestBody,
            String accessToken,
            Class<T> responseType
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        Objects.requireNonNull(requestBody, "Request body must not be null");
        Objects.requireNonNull(responseType, "Response type must not be null");

        return CompletableFuture.supplyAsync(
                () -> executePostJsonBlocking(path, requestBody, accessToken.trim(), responseType),
                executorService
        );
    }

    public <T> CompletableFuture<T> getPublic(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType
    ) {
        return executeGet(path, queryParameters, null, responseType);
    }

    public <T> CompletableFuture<T> getAuthorized(
            String path,
            Map<String, String> queryParameters,
            String accessToken,
            Class<T> responseType
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        return executeGet(path, queryParameters, accessToken.trim(), responseType);
    }

    private <T> CompletableFuture<T> executeGet(
            String path,
            Map<String, String> queryParameters,
            String accessToken,
            Class<T> responseType
    ) {
        Objects.requireNonNull(responseType, "Response type must not be null");

        return CompletableFuture.supplyAsync(
                () -> executeGetBlocking(path, queryParameters, accessToken, responseType),
                executorService
        );
    }

    private <T> T executeGetBlocking(
            String path,
            Map<String, String> queryParameters,
            String accessToken,
            Class<T> responseType
    ) {
        Request request = buildGetRequest(path, queryParameters, accessToken);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = readBody(response.body());

            if (!response.isSuccessful()) {
                log.warn(
                        "HH API GET request failed: path={}, statusCode={}, responseBody={}",
                        path,
                        response.code(),
                        abbreviate(responseBody, 1000)
                );

                throw new HhApiRequestException(
                        buildErrorMessage(response.code()),
                        response.code(),
                        responseBody
                );
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new HhApiRequestException(
                        "HH API response body is empty",
                        response.code(),
                        responseBody
                );
            }

            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException exception) {
            throw new HhApiRequestException("HH API request failed", exception);
        }
    }

    private Request buildGetRequest(
            String path,
            Map<String, String> queryParameters,
            String accessToken
    ) {
        HttpUrl url = buildUrl(path, queryParameters);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .header(USER_AGENT_HEADER, properties.userAgent())
                .header(ACCEPT_HEADER, APPLICATION_JSON);

        if (accessToken != null && !accessToken.isBlank()) {
            requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken);
        }

        return requestBuilder.build();
    }

    private HttpUrl buildUrl(String path, Map<String, String> queryParameters) {
        if (path == null || path.isBlank()) {
            throw new HhApiRequestException("HH API request path must not be blank", -1, null);
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        HttpUrl baseUrl = HttpUrl.parse(properties.baseUrl());

        if (baseUrl == null) {
            throw new HhApiConfigurationException("HH API base URL is invalid");
        }

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder();

        for (String pathSegment : normalizedPath.substring(1).split("/")) {
            if (!pathSegment.isBlank()) {
                urlBuilder.addPathSegment(pathSegment);
            }
        }

        if (queryParameters != null) {
            queryParameters.forEach((name, value) -> {
                if (name != null && !name.isBlank() && value != null) {
                    urlBuilder.addQueryParameter(name, value);
                }
            });
        }

        return urlBuilder.build();
    }

    private <T> T executePostJsonBlocking(
            String path,
            Object requestBody,
            String accessToken,
            Class<T> responseType
    ) {
        Request request = buildPostJsonRequest(path, requestBody, accessToken);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = readBody(response.body());

            if (!response.isSuccessful()) {
                log.warn(
                        "HH API POST request failed: path={}, statusCode={}, responseBody={}",
                        path,
                        response.code(),
                        abbreviate(responseBody, 1000)
                );

                throw new HhApiRequestException(
                        buildErrorMessage(response.code()),
                        response.code(),
                        responseBody
                );
            }

            if (Void.class.equals(responseType)) {
                return null;
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new HhApiRequestException(
                        "HH API response body is empty",
                        response.code(),
                        responseBody
                );
            }

            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException exception) {
            throw new HhApiRequestException("HH API request failed", exception);
        }
    }

    private Request buildPostJsonRequest(
            String path,
            Object requestBody,
            String accessToken
    ) {
        HttpUrl url = buildUrl(path, Map.of());

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(json, APPLICATION_JSON_MEDIA_TYPE);

            return new Request.Builder()
                    .url(url)
                    .post(body)
                    .header(USER_AGENT_HEADER, properties.userAgent())
                    .header(ACCEPT_HEADER, APPLICATION_JSON)
                    .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                    .build();
        } catch (IOException exception) {
            throw new HhApiRequestException("Failed to serialize HH API request body", exception);
        }
    }

    private String readBody(ResponseBody body) throws IOException {
        if (body == null) {
            return null;
        }

        return body.string();
    }

    private String buildErrorMessage(int statusCode) {
        return "HH API request failed with HTTP status " + statusCode;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }

}
