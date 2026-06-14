package ru.jobhunter.infrastructure.platform.hh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class HhOAuthCustomSchemeForwardClient {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthCustomSchemeForwardClient.class);

    private static final int CONNECT_TIMEOUT_MILLIS = 1_000;
    private static final int READ_TIMEOUT_MILLIS = 2_000;
    private static final String END_MARKER = "__END__";

    private HhOAuthCustomSchemeForwardClient() {
    }

    public static boolean forwardToRunningInstanceIfNeeded(String[] args) {
        List<String> callbackUris = HhOAuthCustomSchemeArgumentDetector.findCustomSchemeCallbacks(args);

        if (callbackUris.isEmpty()) {
            return false;
        }

        int forwardPort = HhOAuthCustomSchemeForwardingConfig.resolveForwardPort();

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), forwardPort),
                    CONNECT_TIMEOUT_MILLIS
            );
            socket.setSoTimeout(READ_TIMEOUT_MILLIS);

            try (
                    BufferedWriter writer = new BufferedWriter(
                            new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                    );
                    BufferedReader reader = new BufferedReader(
                            new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                    )
            ) {
                for (String callbackUri : callbackUris) {
                    writer.write(callbackUri);
                    writer.newLine();
                }

                writer.write(END_MARKER);
                writer.newLine();
                writer.flush();

                String response = reader.readLine();

                boolean forwarded = "OK".equalsIgnoreCase(response)
                        || "IGNORED".equalsIgnoreCase(response);

                if (forwarded) {
                    log.info("HH OAuth custom URI callback forwarded to running application instance");
                }

                return forwarded;
            }
        } catch (IOException exception) {
            log.debug("Running JobHunterPro instance was not found for HH OAuth custom URI forwarding");
            return false;
        }
    }
}