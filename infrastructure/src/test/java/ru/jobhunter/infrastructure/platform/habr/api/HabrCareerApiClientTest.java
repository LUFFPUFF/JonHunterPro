package ru.jobhunter.infrastructure.platform.habr.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jobhunter.infrastructure.platform.habr.api.dto.HabrCareerCurrentUserResponse;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class HabrCareerApiClientTest {

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
    void shouldGetCurrentUser() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "login": "habr-user",
                          "email": "habr@example.com",
                          "first_name": "Иван",
                          "last_name": "Иванов",
                          "middle_name": "Иванович",
                          "birthday": "1990-08-15",
                          "avatar": "https://career.habr.com/avatar.png",
                          "location": {
                            "city": "Москва",
                            "country": "Россия"
                          },
                          "gender": "male"
                        }
                        """)
                .build());

        HabrCareerApiClient client = apiClient();

        HabrCareerCurrentUserResponse response = client.getCurrentUser(
                "test-access-token"
        ).join();

        assertEquals("habr-user", response.login());
        assertEquals("habr@example.com", response.email());
        assertEquals("Иван", response.firstName());
        assertEquals("Иванов", response.lastName());
        assertEquals("Иванович", response.middleName());
        assertEquals("1990-08-15", response.birthday());
        assertEquals("https://career.habr.com/avatar.png", response.avatar());
        assertEquals("male", response.gender());

        assertNotNull(response.location());
        assertEquals("Москва", response.location().city());
        assertEquals("Россия", response.location().country());

        RecordedRequest request = server.takeRequest();

        assertTrue(request.getRequestLine().startsWith(
                "GET /api/v1/integrations/users/me?"
        ));
        assertTrue(request.getRequestLine().contains("access_token=test-access-token"));
    }

    @Test
    void shouldRejectBlankAccessToken() {
        HabrCareerApiClient client = apiClient();

        HabrCareerApiConfigurationException exception = assertThrows(
                HabrCareerApiConfigurationException.class,
                () -> client.getCurrentUser(" ")
        );

        assertTrue(exception.getMessage().contains("access token"));
    }

    @Test
    void shouldPropagateUnauthorizedResponse() {
        server.enqueue(new MockResponse.Builder()
                .code(401)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "error": "invalid_token"
                        }
                        """)
                .build());

        HabrCareerApiClient client = apiClient();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> client.getCurrentUser("expired-token").join()
        );

        assertInstanceOf(HabrCareerApiRequestException.class, exception.getCause());

        HabrCareerApiRequestException cause =
                (HabrCareerApiRequestException) exception.getCause();

        assertEquals(401, cause.statusCode());
        assertNotNull(cause.responseBody());
        assertTrue(cause.responseBody().contains("invalid_token"));
    }

    private HabrCareerApiClient apiClient() {
        return new HabrCareerApiClient(
                new HabrCareerApiRequestExecutor(
                        properties(),
                        new OkHttpClient.Builder()
                                .callTimeout(Duration.ofSeconds(5))
                                .build(),
                        new ObjectMapper(),
                        executorService
                )
        );
    }

    private HabrCareerApiProperties properties() {
        return new HabrCareerApiProperties(
                server.url("/api").toString(),
                "JobHunterPro/0.1.0 (test@example.com)",
                true
        );
    }
}