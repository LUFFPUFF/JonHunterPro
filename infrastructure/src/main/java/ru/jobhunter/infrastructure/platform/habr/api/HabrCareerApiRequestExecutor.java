package ru.jobhunter.infrastructure.platform.habr.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component

public class HabrCareerApiRequestExecutor {

    private static final String ACCESS_TOKEN_QUERY_PARAMETER = "access_token";

    private final HabrCareerApiProperties properties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerApiRequestExecutor(
            HabrCareerApiProperties properties,
            @Qualifier("okHttpClient") OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career API properties must not be null"
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

    public <T> CompletableFuture<T> getAuthorized(
            String path,
            Map<String, String> queryParameters,
            String accessToken,
            Class<T> responseType
    ) {
        String normalizedPath = normalizePath(path);
        String normalizedAccessToken = requireNotBlank(
                accessToken,
                "Habr Career API access token must not be blank"
        );

        Objects.requireNonNull(responseType, "Response type must not be null");

        Map<String, String> safeQueryParameters = queryParameters == null
                ? Map.of()
                : Map.copyOf(queryParameters);

        return CompletableFuture.supplyAsync(
                () -> executeGet(
                        normalizedPath,
                        safeQueryParameters,
                        normalizedAccessToken,
                        responseType
                ),
                executorService
        );
    }

    private <T> T executeGet(
            String path,
            Map<String, String> queryParameters,
            String accessToken,
            Class<T> responseType
    ) {
        HttpUrl url = buildUrl(path, queryParameters, accessToken);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", properties.userAgent())
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = readResponseBody(response);

            if (!response.isSuccessful()) {
                throw new HabrCareerApiRequestException(
                        "Habr Career API request failed with HTTP status: " + response.code(),
                        response.code(),
                        responseBody
                );
            }

            return objectMapper.readValue(responseBody, responseType);
        } catch (HabrCareerApiRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new HabrCareerApiRequestException(
                    "Failed to execute Habr Career API request",
                    exception
            );
        }
    }

    private HttpUrl buildUrl(
            String path,
            Map<String, String> queryParameters,
            String accessToken
    ) {
        HttpUrl baseUrl = HttpUrl.parse(properties.baseUrl() + path);

        if (baseUrl == null) {
            throw new HabrCareerApiConfigurationException(
                    "Habr Career API request URL is invalid"
            );
        }

        HttpUrl.Builder builder = baseUrl.newBuilder();

        queryParameters.forEach((name, value) -> {
            if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                builder.addQueryParameter(name.trim(), value.trim());
            }
        });

        builder.addQueryParameter(ACCESS_TOKEN_QUERY_PARAMETER, accessToken);

        return builder.build();
    }

    private String normalizePath(String path) {
        String normalizedPath = requireNotBlank(
                path,
                "Habr Career API request path must not be blank"
        );

        return normalizedPath.startsWith("/")
                ? normalizedPath
                : "/" + normalizedPath;
    }

    private String readResponseBody(Response response) throws IOException {
        ResponseBody body = response.body();

        if (body == null) {
            return "";
        }

        return body.string();
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new HabrCareerApiConfigurationException(message);
        }

        return value.trim();
    }
}
