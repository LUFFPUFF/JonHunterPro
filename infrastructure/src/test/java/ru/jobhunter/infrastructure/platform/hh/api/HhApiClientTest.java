package ru.jobhunter.infrastructure.platform.hh.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhCurrentUserResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchRequest;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchResponse;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HhApiClientTest {

    private static final String USER_AGENT = "JobHunterPro/0.1.0 (test@example.com)";

    private MockWebServer server;
    private ExecutorService executorService;
    private HhApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        executorService = Executors.newVirtualThreadPerTaskExecutor();

        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules();

        HhApiProperties properties = new HhApiProperties(
                server.url("/").toString(),
                USER_AGENT,
                true
        );

        HhApiRequestExecutor requestExecutor = new HhApiRequestExecutor(
                new OkHttpClient(),
                objectMapper,
                properties,
                executorService
        );

        apiClient = new HhApiClient(requestExecutor);
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
    void shouldGetCurrentUser() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("""
                        {
                          "id": "123456",
                          "email": "test@example.com",
                          "first_name": "Ivan",
                          "last_name": "Ivanov",
                          "middle_name": "Ivanovich",
                          "user_type": "employer",
                          "is_admin": false,
                          "unknown_field": "ignored"
                        }
                        """)
                .addHeader("Content-Type", "application/json")
                .build());

        HhCurrentUserResponse response = apiClient.getCurrentUser("test-access-token")
                .get(1, TimeUnit.SECONDS);

        assertEquals("123456", response.id());
        assertEquals("test@example.com", response.email());
        assertEquals("Ivan", response.firstName());
        assertEquals("Ivanov", response.lastName());
        assertEquals("Ivanovich", response.middleName());
        assertEquals("employer", response.userType());
        assertEquals(Boolean.FALSE, response.admin());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(request);
        assertTrue(request.getRequestLine().startsWith("GET /me "));
        assertEquals("Bearer test-access-token", request.getHeaders().get("Authorization"));
        assertEquals(USER_AGENT, request.getHeaders().get("User-Agent"));
        assertEquals("application/json", request.getHeaders().get("Accept"));
    }

    @Test
    void shouldRejectBlankAccessToken() {
        HhApiRequestException exception = assertThrows(
                HhApiRequestException.class,
                () -> apiClient.getCurrentUser(" ")
        );

        assertEquals("HH API access token must not be blank", exception.getMessage());
    }

    @Test
    void shouldPropagateUnauthorizedResponse() {
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
                () -> apiClient.getCurrentUser("expired-token").join()
        );

        assertInstanceOf(HhApiRequestException.class, exception.getCause());

        HhApiRequestException cause = (HhApiRequestException) exception.getCause();

        assertEquals(401, cause.statusCode());
        assertNotNull(cause.responseBody());
        assertTrue(cause.responseBody().contains("token expired"));
    }

    @Test
    void shouldSearchVacancies() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("""
                    {
                      "items": [
                        {
                          "id": "1001",
                          "name": "Java Backend Developer",
                          "url": "https://api.hh.ru/vacancies/1001",
                          "alternate_url": "https://hh.ru/vacancy/1001",
                          "area": {
                            "id": "113",
                            "name": "Россия"
                          },
                          "employer": {
                            "id": "2001",
                            "name": "Test Company",
                            "url": "https://api.hh.ru/employers/2001",
                            "alternate_url": "https://hh.ru/employer/2001"
                          },
                          "salary": {
                            "from": 120000,
                            "to": 200000,
                            "currency": "RUR",
                            "gross": true
                          },
                          "experience": {
                            "id": "between1And3",
                            "name": "От 1 года до 3 лет"
                          },
                          "employment": {
                            "id": "full",
                            "name": "Полная занятость"
                          },
                          "schedule": {
                            "id": "remote",
                            "name": "Удаленная работа"
                          },
                          "unknown_field": "ignored"
                        }
                      ],
                      "found": 1,
                      "pages": 1,
                      "page": 0,
                      "per_page": 20
                    }
                    """)
                .addHeader("Content-Type", "application/json")
                .build());

        HhVacancySearchResponse response = apiClient.searchVacancies(
                new HhVacancySearchRequest(
                        "java backend",
                        "113",
                        0,
                        20
                )
        ).get(1, TimeUnit.SECONDS);

        assertNotNull(response.items());
        assertEquals(1, response.items().size());
        assertEquals(1, response.found());
        assertEquals(1, response.pages());
        assertEquals(0, response.page());
        assertEquals(20, response.perPage());

        var vacancy = response.items().getFirst();

        assertEquals("1001", vacancy.id());
        assertEquals("Java Backend Developer", vacancy.name());
        assertEquals("https://api.hh.ru/vacancies/1001", vacancy.url());
        assertEquals("https://hh.ru/vacancy/1001", vacancy.alternateUrl());

        assertNotNull(vacancy.area());
        assertEquals("113", vacancy.area().id());
        assertEquals("Россия", vacancy.area().name());

        assertNotNull(vacancy.employer());
        assertEquals("2001", vacancy.employer().id());
        assertEquals("Test Company", vacancy.employer().name());

        assertNotNull(vacancy.salary());
        assertEquals(120000, vacancy.salary().from());
        assertEquals(200000, vacancy.salary().to());
        assertEquals("RUR", vacancy.salary().currency());
        assertEquals(Boolean.TRUE, vacancy.salary().gross());

        assertNotNull(vacancy.experience());
        assertEquals("between1And3", vacancy.experience().id());

        assertNotNull(vacancy.employment());
        assertEquals("full", vacancy.employment().id());

        assertNotNull(vacancy.schedule());
        assertEquals("remote", vacancy.schedule().id());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(request);

        String requestLine = request.getRequestLine();

        assertTrue(requestLine.startsWith("GET /vacancies?"));
        assertTrue(requestLine.contains("text=java%20backend"));
        assertTrue(requestLine.contains("area=113"));
        assertTrue(requestLine.contains("page=0"));
        assertTrue(requestLine.contains("per_page=20"));
        assertEquals(USER_AGENT, request.getHeaders().get("User-Agent"));
        assertEquals("application/json", request.getHeaders().get("Accept"));
        assertNull(request.getHeaders().get("Authorization"));
    }

    @Test
    void shouldRejectInvalidVacancySearchPagination() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new HhVacancySearchRequest(
                        "java",
                        "113",
                        -1,
                        20
                )
        );

        assertEquals("HH vacancy search page must not be negative", exception.getMessage());

        IllegalArgumentException perPageException = assertThrows(
                IllegalArgumentException.class,
                () -> new HhVacancySearchRequest(
                        "java",
                        "113",
                        0,
                        101
                )
        );

        assertEquals("HH vacancy search perPage must be between 1 and 100", perPageException.getMessage());
    }
}