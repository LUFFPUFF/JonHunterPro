package ru.jobhunter.infrastructure.platform.hh.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HH OAuth token client.
 */
class HhOAuthTokenClientTest {

    @Test
    void shouldExchangeAuthorizationCode() throws Exception {
        try (
                MockWebServer server = new MockWebServer();
                var executor = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            server.start();

            server.enqueue(new MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""
                            {
                              "access_token": "access-token",
                              "refresh_token": "refresh-token",
                              "token_type": "bearer",
                              "expires_in": 3600,
                              "scope": "employer"
                            }
                            """)
                    .build());

            HhOAuthProperties properties = new HhOAuthProperties(
                    "https://hh.ru/oauth/authorize",
                    server.url("/token").toString(),
                    "client-id",
                    "client-secret",
                    "http://127.0.0.1:54345/oauth/hh/callback",
                    54345,
                    32,
                    "JobHunterPro/0.1.0 (test@example.com)"
            );

            HhOAuthTokenClient client = new HhOAuthTokenClient(
                    new OkHttpClient(),
                    new ObjectMapper(),
                    properties,
                    executor
            );

            HhOAuthTokenResponse response = client.exchangeAuthorizationCode("auth-code").join();

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.tokenType()).isEqualTo("bearer");
            assertThat(response.expiresIn()).isEqualTo(3600);
            assertThat(response.scope()).isEqualTo("employer");

            var recordedRequest = server.takeRequest();

            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getRequestLine()).startsWith("POST /token ");
            assertThat(recordedRequest.getHeaders().get("User-Agent"))
                    .isEqualTo("JobHunterPro/0.1.0 (test@example.com)");

            String body = recordedRequest.getBody().utf8();

            assertThat(body).contains("grant_type=authorization_code");
            assertThat(body).contains("client_id=client-id");
            assertThat(body).contains("client_secret=client-secret");
            assertThat(body).contains("code=auth-code");
            assertThat(body).contains("redirect_uri=http%3A%2F%2F127.0.0.1%3A54345%2Foauth%2Fhh%2Fcallback");
        }
    }

    @Test
    void shouldRefreshToken() throws Exception {
        try (
                MockWebServer server = new MockWebServer();
                var executor = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            server.start();

            server.enqueue(new MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""
                            {
                              "access_token": "new-access-token",
                              "refresh_token": "new-refresh-token",
                              "token_type": "bearer",
                              "expires_in": 3600,
                              "scope": "employer"
                            }
                            """)
                    .build());

            HhOAuthProperties properties = new HhOAuthProperties(
                    "https://hh.ru/oauth/authorize",
                    server.url("/token").toString(),
                    "client-id",
                    "client-secret",
                    "http://127.0.0.1:54345/oauth/hh/callback",
                    54345,
                    32,
                    "JobHunterPro/0.1.0 (test@example.com)"
            );

            HhOAuthTokenClient client = new HhOAuthTokenClient(
                    new OkHttpClient(),
                    new ObjectMapper(),
                    properties,
                    executor
            );

            HhOAuthTokenResponse response = client.refreshToken("old-refresh-token").join();

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.tokenType()).isEqualTo("bearer");
            assertThat(response.expiresIn()).isEqualTo(3600);
            assertThat(response.scope()).isEqualTo("employer");

            var recordedRequest = server.takeRequest();

            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getRequestLine()).startsWith("POST /token ");
            assertThat(recordedRequest.getHeaders().get("User-Agent"))
                    .isEqualTo("JobHunterPro/0.1.0 (test@example.com)");

            String body = recordedRequest.getBody().utf8();

            assertThat(body).contains("grant_type=refresh_token");
            assertThat(body).contains("refresh_token=old-refresh-token");
            assertThat(body).doesNotContain("client_secret=client-secret");
            assertThat(body).doesNotContain("authorization_code");
        }
    }
}