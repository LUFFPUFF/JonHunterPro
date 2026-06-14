package ru.jobhunter.infrastructure.platform.hh.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class HhOAuthCallbackServerTest {

    @Test
    void shouldReceiveAuthorizationCode() throws Exception {
        int port = findFreePort();
        String host = "127.0.0.1";

        HhOAuthProperties properties = new HhOAuthProperties(
                "https://hh.ru/oauth/authorize",
                "https://api.hh.ru/token",
                "client-id",
                "client-secret",
                "http://" + host + ":" + port + "/oauth/hh/callback",
                HhOAuthRedirectMode.LOCAL_HTTP_SERVER.name(),
                port,
                32,
                "JobHunterPro/0.1.0 (test@example.com)"
        );

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            HhOAuthCallbackServer server = new HhOAuthCallbackServer(properties, executor);

            var future = server.waitForCallback("expected-state", Duration.ofSeconds(5));

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://" + host + ":" + port + "/oauth/hh/callback"
                                    + "?code=test-code"
                                    + "&state=expected-state"
                    ))
                    .GET()
                    .build();

            HttpResponse<String> response = sendWithRetry(client, request);

            HhOAuthCallbackResult result = future.join();

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(result.code()).isEqualTo("test-code");
            assertThat(result.state()).isEqualTo("expected-state");
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static HttpResponse<String> sendWithRetry(
            HttpClient client,
            HttpRequest request
    ) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                lastException = exception;
                Thread.sleep(100);
            }
        }

        throw lastException;
    }
}