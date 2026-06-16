package ru.jobhunter.infrastructure.platform.habr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class HabrCareerApiRequestExecutorTest {

    private MockWebServer server;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {
        executorService.close();
        server.close();
    }

    @Test
    void shouldExecuteAuthorizedGetRequest() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "login": "test-user",
                          "email": "test@example.com"
                        }
                        """)
                .build());

        HabrCareerApiRequestExecutor executor = requestExecutor();

        TestCurrentUserResponse response = executor.getAuthorized(
                "/v1/integrations/users/me",
                Map.of(),
                "test-access-token",
                TestCurrentUserResponse.class
        ).join();

        assertEquals("test-user", response.login());
        assertEquals("test@example.com", response.email());

        RecordedRequest request = server.takeRequest();

        assertTrue(request.getRequestLine().startsWith("GET /api/v1/integrations/users/me?"));
        assertTrue(request.getRequestLine().contains("access_token=test-access-token"));
        assertEquals(
                "JobHunterPro/0.1.0 (test@example.com)",
                request.getHeaders().get("User-Agent")
        );
        assertEquals("application/json", request.getHeaders().get("Accept"));
    }

    @Test
    void shouldAppendQueryParameters() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "login": "test-user",
                          "email": "test@example.com"
                        }
                        """)
                .build());

        HabrCareerApiRequestExecutor executor = requestExecutor();

        executor.getAuthorized(
                "/v1/integrations/users/me",
                Map.of("page", "2", "term", "java"),
                "test-access-token",
                TestCurrentUserResponse.class
        ).join();

        RecordedRequest request = server.takeRequest();

        String requestLine = request.getRequestLine();

        assertTrue(requestLine.contains("page=2"));
        assertTrue(requestLine.contains("term=java"));
        assertTrue(requestLine.contains("access_token=test-access-token"));
    }

    @Test
    void shouldThrowExceptionWhenResponseStatusIsNotSuccessful() {
        server.enqueue(new MockResponse.Builder()
                .code(401)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "error": "invalid_token"
                        }
                        """)
                .build());

        HabrCareerApiRequestExecutor executor = requestExecutor();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> executor.getAuthorized(
                        "/v1/integrations/users/me",
                        Map.of(),
                        "expired-token",
                        TestCurrentUserResponse.class
                ).join()
        );

        assertInstanceOf(HabrCareerApiRequestException.class, exception.getCause());

        HabrCareerApiRequestException cause =
                (HabrCareerApiRequestException) exception.getCause();

        assertEquals(401, cause.statusCode());
        assertNotNull(cause.responseBody());
        assertTrue(cause.responseBody().contains("invalid_token"));
    }

    @Test
    void shouldRejectBlankAccessToken() {
        HabrCareerApiRequestExecutor executor = requestExecutor();

        HabrCareerApiConfigurationException exception = assertThrows(
                HabrCareerApiConfigurationException.class,
                () -> executor.getAuthorized(
                        "/v1/integrations/users/me",
                        Map.of(),
                        " ",
                        TestCurrentUserResponse.class
                )
        );

        assertTrue(exception.getMessage().contains("access token"));
    }

    private HabrCareerApiRequestExecutor requestExecutor() {
        return new HabrCareerApiRequestExecutor(
                properties(),
                new OkHttpClient.Builder()
                        .callTimeout(Duration.ofSeconds(5))
                        .build(),
                new ObjectMapper(),
                executorService
        );
    }

    private HabrCareerApiProperties properties() {
        return new HabrCareerApiProperties(
                server.url("/api").toString(),
                "JobHunterPro/0.1.0 (test@example.com)",
                true
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TestCurrentUserResponse(
            String login,
            String email
    ) {
    }
}