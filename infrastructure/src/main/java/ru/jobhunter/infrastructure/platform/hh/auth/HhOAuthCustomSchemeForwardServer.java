package ru.jobhunter.infrastructure.platform.hh.auth;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.oauth.OAuthCustomSchemeCallbackDispatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public final class HhOAuthCustomSchemeForwardServer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthCustomSchemeForwardServer.class);

    private static final String END_MARKER = "__END__";

    private final HhOAuthProperties properties;
    private final OAuthCustomSchemeCallbackDispatcher callbackDispatcher;

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public HhOAuthCustomSchemeForwardServer(
            HhOAuthProperties properties,
            OAuthCustomSchemeCallbackDispatcher callbackDispatcher
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "HH OAuth properties must not be null"
        );
        this.callbackDispatcher = Objects.requireNonNull(
                callbackDispatcher,
                "OAuth custom scheme callback dispatcher must not be null"
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.parsedRedirectMode() != HhOAuthRedirectMode.CUSTOM_URI_SCHEME) {
            return;
        }

        startServer();
    }

    private void startServer() {
        int forwardPort = HhOAuthCustomSchemeForwardingConfig.resolveForwardPort();

        try {
            ServerSocket socket = new ServerSocket();
            socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), forwardPort));

            this.serverSocket = socket;
            this.acceptThread = Thread.ofVirtual()
                    .name("hh-oauth-custom-scheme-forward-server")
                    .start(() -> acceptLoop(socket));

            log.info("HH OAuth custom URI forward server started: port={}", forwardPort);
        } catch (IOException exception) {
            throw new HhOAuthCallbackException(
                    "Failed to start HH OAuth custom URI forward server on port " + forwardPort,
                    exception
            );
        }
    }

    private void acceptLoop(ServerSocket socket) {
        while (!socket.isClosed()) {
            try {
                Socket clientSocket = socket.accept();

                Thread.ofVirtual()
                        .name("hh-oauth-custom-scheme-forward-client")
                        .start(() -> handleClient(clientSocket));
            } catch (SocketException exception) {
                if (!socket.isClosed()) {
                    log.warn("HH OAuth custom URI forward server socket error", exception);
                }
            } catch (IOException exception) {
                log.warn("Failed to accept HH OAuth custom URI forward connection", exception);
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                Socket socket = clientSocket;
                BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                BufferedWriter writer = new BufferedWriter(
                        new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                )
        ) {
            int dispatchedCount = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (END_MARKER.equals(line)) {
                    break;
                }

                if (callbackDispatcher.dispatchIfSupported(line)) {
                    dispatchedCount++;
                }
            }

            writer.write(dispatchedCount > 0 ? "OK" : "IGNORED");
            writer.newLine();
            writer.flush();

            log.info("HH OAuth custom URI forwarded callbacks processed: count={}", dispatchedCount);
        } catch (RuntimeException exception) {
            log.warn("Failed to dispatch HH OAuth custom URI callback", exception);
        } catch (IOException exception) {
            log.warn("Failed to handle HH OAuth custom URI forward connection", exception);
        }
    }

    @PreDestroy
    public void stopServer() {
        ServerSocket socket = this.serverSocket;

        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            socket.close();
            log.info("HH OAuth custom URI forward server stopped");
        } catch (IOException exception) {
            log.warn("Failed to stop HH OAuth custom URI forward server", exception);
        }
    }
}