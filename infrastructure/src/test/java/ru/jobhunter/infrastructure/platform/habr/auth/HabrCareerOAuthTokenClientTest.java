package ru.jobhunter.infrastructure.platform.habr.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class HabrCareerOAuthTokenClientTest {

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
    void shouldExchangeAuthorizationCodeForAccessToken() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "access_token": "test-habr-access-token"
                        }
                        """)
                .build());

        HabrCareerOAuthTokenClient client = tokenClient();

        HabrCareerOAuthTokenResponse response = client.exchangeAuthorizationCode(
                "authorization-code-123"
        ).join();

        assertEquals("test-habr-access-token", response.accessToken());

        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(recordedRequest.getRequestLine().startsWith("POST /token "));
        assertEquals(
                "JobHunterPro/0.1.0 (test@example.com)",
                recordedRequest.getHeaders().get("User-Agent")
        );

        String decodedBody = URLDecoder.decode(
                recordedRequest.getBody().utf8(),
                StandardCharsets.UTF_8
        );

        assertTrue(decodedBody.contains("grant_type=authorization_code"));
        assertTrue(decodedBody.contains("client_id=test-client-id"));
        assertTrue(decodedBody.contains("client_secret=test-client-secret"));
        assertTrue(decodedBody.contains("redirect_uri=jobhunterpro://oauth/habr/callback"));
        assertTrue(decodedBody.contains("code=authorization-code-123"));
    }

    @Test
    void shouldThrowExceptionWhenResponseStatusIsNotSuccessful() {
        server.enqueue(new MockResponse.Builder()
                .code(400)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "error": "invalid_request",
                          "error_description": "bad redirect url"
                        }
                        """)
                .build());

        HabrCareerOAuthTokenClient client = tokenClient();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> client.exchangeAuthorizationCode("bad-code").join()
        );

        assertInstanceOf(HabrCareerOAuthTokenRequestException.class, exception.getCause());

        HabrCareerOAuthTokenRequestException cause =
                (HabrCareerOAuthTokenRequestException) exception.getCause();

        assertEquals(400, cause.statusCode());
        assertNotNull(cause.responseBody());
        assertTrue(cause.responseBody().contains("invalid_request"));
    }

    @Test
    void shouldRejectBlankAuthorizationCode() {
        HabrCareerOAuthTokenClient client = tokenClient();

        HabrCareerOAuthConfigurationException exception = assertThrows(
                HabrCareerOAuthConfigurationException.class,
                () -> client.exchangeAuthorizationCode(" ")
        );

        assertTrue(exception.getMessage().contains("authorization code"));
    }

    private HabrCareerOAuthTokenClient tokenClient() {
        return new HabrCareerOAuthTokenClient(
                properties(),
                new OkHttpClient.Builder()
                        .callTimeout(Duration.ofSeconds(5))
                        .build(),
                new ObjectMapper(),
                executorService
        );
    }

    private HabrCareerOAuthProperties properties() {
        return new HabrCareerOAuthProperties(
                "https://career.habr.com/integrations/oauth/authorize",
                server.url("/token").toString(),
                "test-client-id",
                "test-client-secret",
                "jobhunterpro://oauth/habr/callback",
                true,
                32,
                "JobHunterPro/0.1.0 (test@example.com)"
        );
    }
}