package ru.jobhunter.infrastructure.platform.hh.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Component
public final class HhOAuthCallbackServer {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthCallbackServer.class);

    private static final String HTTP_GET = "GET";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final HhOAuthProperties properties;
    private final Executor executor;
    private final AtomicReference<RunningCallback> runningCallback = new AtomicReference<>();

    public HhOAuthCallbackServer(
            HhOAuthProperties properties,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.properties = Objects.requireNonNull(properties, "HH OAuth properties must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
    }

    public CompletableFuture<HhOAuthCallbackResult> waitForCallback(String expectedState) {
        return waitForCallback(expectedState, DEFAULT_TIMEOUT);
    }

    public CompletableFuture<HhOAuthCallbackResult> waitForCallback(
            String expectedState,
            Duration timeout
    ) {
        if (expectedState == null || expectedState.isBlank()) {
            throw new IllegalArgumentException("Expected OAuth state must not be blank");
        }

        Objects.requireNonNull(timeout, "Timeout must not be null");

        CompletableFuture<HhOAuthCallbackResult> callbackFuture = new CompletableFuture<>();

        try {
            HttpServer server = createServer(expectedState.trim(), callbackFuture);
            RunningCallback running = new RunningCallback(server, callbackFuture);

            if (!runningCallback.compareAndSet(null, running)) {
                throw new HhOAuthCallbackException("HH OAuth callback server is already running");
            }

            server.start();
            log.info(
                    "HH OAuth callback server started: port={}, redirectUri={}",
                    properties.callbackPort(),
                    properties.redirectUri()
            );

            callbackFuture.whenComplete((result, throwable) -> stopRunningServer());

            CompletableFuture.runAsync(() -> waitAndTimeout(callbackFuture, timeout), executor);

            return callbackFuture;
        } catch (IOException exception) {
            throw new HhOAuthCallbackException("Failed to start HH OAuth callback server", exception);
        } catch (RuntimeException exception) {
            callbackFuture.completeExceptionally(exception);
            throw exception;
        }
    }

    public void stop() {
        stopRunningServer();
    }

    private HttpServer createServer(
            String expectedState,
            CompletableFuture<HhOAuthCallbackResult> callbackFuture
    ) throws IOException {
        validateConfiguration();

        URI redirectUri = URI.create(properties.redirectUri());
        String callbackPath = redirectUri.getPath();

        if (callbackPath == null || callbackPath.isBlank()) {
            throw new HhOAuthConfigurationException("HH redirect URI must contain callback path");
        }

        HttpServer server = HttpServer.create(
                new InetSocketAddress(redirectUri.getHost(), properties.callbackPort()),
                0
        );

        server.createContext(callbackPath, exchange -> handleCallback(exchange, expectedState, callbackFuture));
        server.setExecutor(executor);

        return server;
    }

    private void handleCallback(
            HttpExchange exchange,
            String expectedState,
            CompletableFuture<HhOAuthCallbackResult> callbackFuture
    ) {
        try {
            if (!HTTP_GET.equalsIgnoreCase(exchange.getRequestMethod())) {
                sendHtml(exchange, 405, "Метод не поддерживается", "Используйте GET redirect от hh.ru.");
                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("Unsupported HH OAuth callback HTTP method")
                );
                return;
            }

            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String error = queryParams.get("error");
            if (error != null && !error.isBlank()) {
                String description = queryParams.getOrDefault(
                        "error_description",
                        "HH.ru вернул ошибку авторизации."
                );

                sendHtml(exchange, 400, "Авторизация HH.ru отклонена", description);

                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth error: " + error + ". " + description)
                );
                return;
            }

            String code = queryParams.get("code");
            String actualState = queryParams.get("state");

            if (code == null || code.isBlank()) {
                sendHtml(exchange, 400, "Ошибка авторизации", "В callback отсутствует authorization code.");

                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth callback does not contain authorization code")
                );
                return;
            }

            if (actualState == null || actualState.isBlank()) {
                sendHtml(exchange, 400, "Ошибка авторизации", "В callback отсутствует state.");

                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth callback does not contain state")
                );
                return;
            }

            if (!expectedState.equals(actualState)) {
                sendHtml(exchange, 400, "Ошибка безопасности", "OAuth state не совпадает.");

                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth state mismatch")
                );
                return;
            }

            HhOAuthCallbackResult result = new HhOAuthCallbackResult(code, actualState);

            sendHtml(
                    exchange,
                    200,
                    "HH.ru подключён",
                    "Код авторизации получен. Можно закрыть эту вкладку и вернуться в JobHunterPro."
            );

            callbackFuture.complete(result);

            log.info("HH OAuth authorization code received successfully");
        } catch (RuntimeException exception) {
            try {
                sendHtml(
                        exchange,
                        500,
                        "Ошибка обработки OAuth callback",
                        "JobHunterPro не смог обработать ответ от HH.ru."
                );
            } catch (IOException ioException) {
                log.warn("Failed to send OAuth error response", ioException);
            }

            callbackFuture.completeExceptionally(exception);
        } catch (IOException exception) {
            callbackFuture.completeExceptionally(
                    new HhOAuthCallbackException("Failed to write HH OAuth callback response", exception)
            );
        }
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>();

        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");

        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }

            int separatorIndex = pair.indexOf('=');

            String rawKey = separatorIndex >= 0
                    ? pair.substring(0, separatorIndex)
                    : pair;

            String rawValue = separatorIndex >= 0
                    ? pair.substring(separatorIndex + 1)
                    : "";

            String key = decode(rawKey);
            String value = decode(rawValue);

            params.put(key, value);
        }

        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendHtml(
            HttpExchange exchange,
            int statusCode,
            String title,
            String message
    ) throws IOException {
        String body = """
                <!doctype html>
                <html lang="ru">
                <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 48px;
                            line-height: 1.5;
                            color: #1f2933;
                        }
                        .card {
                            max-width: 680px;
                            border: 1px solid #d8dee4;
                            border-radius: 12px;
                            padding: 28px;
                            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
                        }
                        h1 {
                            margin-top: 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>%s</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(title), escapeHtml(message));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void waitAndTimeout(
            CompletableFuture<HhOAuthCallbackResult> callbackFuture,
            Duration timeout
    ) {
        try {
            Thread.sleep(timeout.toMillis());

            if (!callbackFuture.isDone()) {
                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth callback timeout exceeded")
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            if (!callbackFuture.isDone()) {
                callbackFuture.completeExceptionally(
                        new HhOAuthCallbackException("HH OAuth callback waiting interrupted", exception)
                );
            }
        }
    }

    private void stopRunningServer() {
        RunningCallback running = runningCallback.getAndSet(null);

        if (running == null) {
            return;
        }

        running.server().stop(0);
        log.info("HH OAuth callback server stopped");
    }

    private void validateConfiguration() {
        if (properties.callbackPort() <= 0 || properties.callbackPort() > 65535) {
            throw new HhOAuthConfigurationException("HH OAuth callback port is invalid");
        }

        if (properties.redirectUri() == null || properties.redirectUri().isBlank()) {
            throw new HhOAuthConfigurationException("HH redirect URI is not configured");
        }

        URI redirectUri = URI.create(properties.redirectUri());

        if (!"http".equalsIgnoreCase(redirectUri.getScheme())) {
            throw new HhOAuthConfigurationException("HH local redirect URI must use http scheme");
        }

        String host = redirectUri.getHost();

        if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
            throw new HhOAuthConfigurationException("HH local redirect URI must use localhost or 127.0.0.1 host");
        }

        if (redirectUri.getPort() != properties.callbackPort()) {
            throw new HhOAuthConfigurationException("HH redirect URI port must match callback port");
        }
    }

    private record RunningCallback(
            HttpServer server,
            CompletableFuture<HhOAuthCallbackResult> future
    ) {
    }
}