package ru.jobhunter.infrastructure.platform.hh.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HhApiRequestExecutorTest {

    private static final String USER_AGENT = "JobHunterPro/0.1.0 (test@example.com)";

    private MockWebServer server;
    private ExecutorService executorService;
    private HhApiRequestExecutor requestExecutor;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        executorService = Executors.newVirtualThreadPerTaskExecutor();

        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        HhApiProperties properties = new HhApiProperties(
                server.url("/").toString(),
                USER_AGENT,
                true
        );

        requestExecutor = new HhApiRequestExecutor(
                new OkHttpClient(),
                objectMapper,
                properties,
                executorService
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (executorService != null) {
            executorService.close();
        }

        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldExecutePublicGetRequest() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("""
                        {
                          "id": "113",
                          "name": "Россия"
                        }
                        """)
                .addHeader("Content-Type", "application/json")
                .build());

        TestAreaResponse response = requestExecutor.getPublic(
                "/areas/113",
                Map.of(),
                TestAreaResponse.class
        ).get(1, TimeUnit.SECONDS);

        assertEquals("113", response.id());
        assertEquals("Россия", response.name());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(request);
        assertThatRequestLineStartsWith(request, "GET /areas/113 ");
        assertEquals(USER_AGENT, request.getHeaders().get("User-Agent"));
        assertEquals("application/json", request.getHeaders().get("Accept"));
        assertNull(request.getHeaders().get("Authorization"));
    }

    @Test
    void shouldExecuteAuthorizedGetRequest() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("""
                        {
                          "id": "user-1",
                          "email": "test@example.com"
                        }
                        """)
                .addHeader("Content-Type", "application/json")
                .build());

        TestMeResponse response = requestExecutor.getAuthorized(
                "/me",
                Map.of(),
                "test-access-token",
                TestMeResponse.class
        ).get(1, TimeUnit.SECONDS);

        assertEquals("user-1", response.id());
        assertEquals("test@example.com", response.email());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(request);
        assertThatRequestLineStartsWith(request, "GET /me ");
        assertEquals("Bearer test-access-token", request.getHeaders().get("Authorization"));
        assertEquals(USER_AGENT, request.getHeaders().get("User-Agent"));
    }

    @Test
    void shouldAppendQueryParameters() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("""
                        {
                          "found": 10,
                          "page": 0,
                          "per_page": 20
                        }
                        """)
                .addHeader("Content-Type", "application/json")
                .build());

        TestVacancySearchResponse response = requestExecutor.getPublic(
                "/vacancies",
                Map.of(
                        "text", "java developer",
                        "area", "113",
                        "page", "0",
                        "per_page", "20"
                ),
                TestVacancySearchResponse.class
        ).get(1, TimeUnit.SECONDS);

        assertEquals(10, response.found());
        assertEquals(0, response.page());
        assertEquals(20, response.perPage());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(request);

        String target = request.getTarget();

        assertTrue(target.startsWith("/vacancies?"));
        assertTrue(target.contains("text=java%20developer"));
        assertTrue(target.contains("area=113"));
        assertTrue(target.contains("page=0"));
        assertTrue(target.contains("per_page=20"));
    }

    @Test
    void shouldThrowExceptionWhenResponseStatusIsNotSuccessful() {
        server.enqueue(new MockResponse.Builder()
                .code(401)
                .body("""
                    {
                      "description": "token expired",
                      "errors": []
                    }
                    """)
                .addHeader("Content-Type", "application/json")
                .build());

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> requestExecutor.getAuthorized(
                        "/me",
                        Map.of(),
                        "expired-token",
                        TestMeResponse.class
                ).join()
        );

        assertInstanceOf(HhApiRequestException.class, exception.getCause());

        HhApiRequestException cause = (HhApiRequestException) exception.getCause();

        assertEquals(401, cause.statusCode());
        assertNotNull(cause.responseBody());
        assertTrue(cause.responseBody().contains("token expired"));
    }

    @Test
    void shouldRejectBlankAccessToken() {
        HhApiRequestException exception = assertThrows(
                HhApiRequestException.class,
                () -> requestExecutor.getAuthorized(
                        "/me",
                        Map.of(),
                        " ",
                        TestMeResponse.class
                )
        );

        assertEquals("HH API access token must not be blank", exception.getMessage());
    }

    private void assertThatRequestLineStartsWith(
            RecordedRequest request,
            String expectedPrefix
    ) {
        assertTrue(
                request.getRequestLine().startsWith(expectedPrefix),
                "Expected request line to start with '" + expectedPrefix
                        + "', but was '" + request.getRequestLine() + "'"
        );
    }

    private record TestAreaResponse(
            String id,
            String name
    ) {
    }

    private record TestMeResponse(
            String id,
            String email
    ) {
    }

    private record TestVacancySearchResponse(
            int found,
            int page,
            int perPage
    ) {
    }
}