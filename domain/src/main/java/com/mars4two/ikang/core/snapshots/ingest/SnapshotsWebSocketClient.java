package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.config.HttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SnapshotsWebSocketClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SnapshotsWebSocketClient.class);

    private final HttpClient httpClient;
    private final Supplier<URI> uriSupplier;
    private final Supplier<Optional<String>> accessTokenSupplier;
    private volatile WebSocket webSocket;
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    public SnapshotsWebSocketClient(HttpClient httpClient) {
        this(httpClient, HttpConfig::getSnapshotsWebSocketUri, AuthService::getAccessToken);
    }

    public SnapshotsWebSocketClient(
            HttpClient httpClient,
            Supplier<URI> uriSupplier,
            Supplier<Optional<String>> accessTokenSupplier
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.uriSupplier = Objects.requireNonNull(uriSupplier, "uriSupplier");
        this.accessTokenSupplier = Objects.requireNonNull(accessTokenSupplier, "accessTokenSupplier");
    }

    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed();
    }

    public void connectAsync() {
        if (isConnected() || !connecting.compareAndSet(false, true)) {
            return;
        }

        var tokenOpt = accessTokenSupplier.get();
        if (tokenOpt.isEmpty()) {
            log.debug("Cannot connect WebSocket: not authenticated yet");
            connecting.set(false);
            return;
        }

        try {
            var uri = uriSupplier.get();
            log.info("Connecting snapshots WebSocket to {}...", uri);

            HttpConfig.newBackendWebSocketBuilder(httpClient)
                    .header("Authorization", "Bearer " + tokenOpt.get())
                    .buildAsync(uri, new WebSocketListener())
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        this.connecting.set(false);
                        log.info("Snapshots WebSocket connection established");
                    })
                    .exceptionally(ex -> {
                        log.warn("Failed to establish snapshots WebSocket connection: {}", ex.getMessage());
                        this.webSocket = null;
                        this.connecting.set(false);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Unexpected error initiating WebSocket connection", e);
            connecting.set(false);
        }
    }

    public boolean send(String jsonPayload) {
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isOutputClosed()) {
            try {
                ws.sendText(jsonPayload, true);
                return true;
            } catch (Exception e) {
                log.warn("Error sending payload through WebSocket: {}", e.getMessage());
                this.webSocket = null;
                connectAsync();
                return false;
            }
        }
        connectAsync();
        return false;
    }

    @Override
    public void close() {
        WebSocket ws = this.webSocket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "Mod shutdown");
            } catch (Exception e) {
                log.debug("Error closing WebSocket", e);
            } finally {
                this.webSocket = null;
            }
        }
    }

    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket ws) {
            log.info("Snapshots WebSocket channel opened");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            log.debug("Snapshots WebSocket response received: {}", data);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("Snapshots WebSocket closed by remote (code: {}, reason: {})", statusCode, reason);
            webSocket = null;
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.warn("Snapshots WebSocket encountered error: {}", error.getMessage());
            webSocket = null;
        }
    }
}
