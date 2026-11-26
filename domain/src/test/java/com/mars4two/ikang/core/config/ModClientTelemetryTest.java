package com.mars4two.ikang.core.config;

import com.mars4two.ikang.core.preferences.WorldPreferencesService;
import com.mars4two.ikang.core.preferences.dtos.WorldPreferencesResponse;
import com.mars4two.ikang.core.snapshots.ingest.SnapshotsWebSocketClient;
import com.mars4two.ikang.core.version.UpdateCheckerService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModClientTelemetryTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.EMPTY);
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void normalizesValuesAndOmitsUnknownOrBlankFields() {
        ModClientTelemetry release = ModClientTelemetry.of(" 26.3 ", " 1.0.0+26.3 ", "Fabric");
        assertTrue(release.hasAnyValue());
        assertEquals("26.3", release.gameVersion());
        assertEquals("1.0.0", release.modVersion());
        assertEquals("fabric", release.modLoader());
        assertEquals(
                Map.of(
                        ModClientTelemetry.HEADER_GAME_VERSION, "26.3",
                        ModClientTelemetry.HEADER_MOD_VERSION, "1.0.0",
                        ModClientTelemetry.HEADER_MOD_LOADER, "fabric"
                ),
                release.toHeaders()
        );

        ModClientTelemetry preRelease = ModClientTelemetry.of("26.3-rc-1", "1.2.0-beta.1+26.3", "  NeoForge ");
        assertEquals("26.3-rc-1", preRelease.gameVersion());
        assertEquals("1.2.0-beta.1", preRelease.modVersion());
        assertEquals("neoforge", preRelease.modLoader());

        ModClientTelemetry empty = ModClientTelemetry.of("unknown", "   ", null);
        assertEquals(ModClientTelemetry.EMPTY, empty);
        assertFalse(empty.hasAnyValue());
        assertNull(empty.gameVersion());
        assertNull(empty.modVersion());
        assertNull(empty.modLoader());
        assertTrue(empty.toHeaders().isEmpty());
    }

    @Test
    void backendRequestBuilderAppliesConfiguredTelemetryAndOmitsAbsentValues() {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("1.21.4", "1.0.0+1.21.4", "Fabric"));
        HttpRequest fullRequest = HttpConfig.newBackendRequestBuilder(HttpConfig.getModCredentialsUri())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        assertEquals(Optional.of("1.21.4"), fullRequest.headers().firstValue(ModClientTelemetry.HEADER_GAME_VERSION));
        assertEquals(Optional.of("1.0.0"), fullRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_VERSION));
        assertEquals(Optional.of("fabric"), fullRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_LOADER));

        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("unknown", "1.0.0+26.3", "NeoForge"));
        HttpRequest partialRequest = HttpConfig.newBackendRequestBuilder(HttpConfig.getRefreshAccessTokenUri())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        assertEquals(Optional.empty(), partialRequest.headers().firstValue(ModClientTelemetry.HEADER_GAME_VERSION));
        assertEquals(Optional.of("1.0.0"), partialRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_VERSION));
        assertEquals(Optional.of("neoforge"), partialRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_LOADER));

        HttpConfig.configureClientTelemetry(null);
        HttpRequest emptyRequest = HttpConfig.newBackendRequestBuilder(HttpConfig.getSendPlayerAuthorizationUri())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        assertEquals(Optional.empty(), emptyRequest.headers().firstValue(ModClientTelemetry.HEADER_GAME_VERSION));
        assertEquals(Optional.empty(), emptyRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_VERSION));
        assertEquals(Optional.empty(), emptyRequest.headers().firstValue(ModClientTelemetry.HEADER_MOD_LOADER));
    }

    @Test
    void worldPreferencesServiceSendsTelemetryHeadersOnGetAndPut() throws IOException {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("26.3", "1.0.0+26.3", "Fabric"));
        UUID serverId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();

        List<Map<String, String>> capturedHeaders = new CopyOnWriteArrayList<>();

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/core/mod-worker/preferences", exchange -> {
            Headers headers = exchange.getRequestHeaders();
            Map<String, String> snapshot = new LinkedHashMap<>();
            snapshot.put("method", exchange.getRequestMethod());
            snapshot.put(ModClientTelemetry.HEADER_GAME_VERSION, headers.getFirst(ModClientTelemetry.HEADER_GAME_VERSION));
            snapshot.put(ModClientTelemetry.HEADER_MOD_VERSION, headers.getFirst(ModClientTelemetry.HEADER_MOD_VERSION));
            snapshot.put(ModClientTelemetry.HEADER_MOD_LOADER, headers.getFirst(ModClientTelemetry.HEADER_MOD_LOADER));
            capturedHeaders.add(snapshot);

            String body = GsonConfig.toJson(new WorldPreferencesResponse(
                    worldId, serverId, true, Map.of("playerStats", true), "2026-11-24T12:00:00Z"
            ));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        WorldPreferencesService service = new WorldPreferencesService(
                HttpClient.newHttpClient(),
                (sId, aId) -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences?serverId=" + sId + "&adminProfileId=" + aId),
                () -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences")
        );

        assertTrue(service.fetchPreferences(serverId, adminId, "token-1", Map.of()).isSuccess());
        assertTrue(service.replacePreferences(serverId, adminId, Map.of("playerStats", true), "token-1", Map.of()).isSuccess());

        assertEquals(2, capturedHeaders.size());
        for (Map<String, String> entry : capturedHeaders) {
            assertEquals("26.3", entry.get(ModClientTelemetry.HEADER_GAME_VERSION));
            assertEquals("1.0.0", entry.get(ModClientTelemetry.HEADER_MOD_VERSION));
            assertEquals("fabric", entry.get(ModClientTelemetry.HEADER_MOD_LOADER));
        }
    }

    @Test
    void snapshotsWebSocketClientSendsTelemetryHeadersInUpgradeHandshake() throws Exception {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("26.3", "1.0.0+26.3", "NeoForge"));

        CountDownLatch headersCaptured = new CountDownLatch(1);
        AtomicReference<Map<String, String>> handshakeHeaders = new AtomicReference<>(Map.of());

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            int port = serverSocket.getLocalPort();
            ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
            try {
                acceptExecutor.submit(() -> {
                    try (Socket socket = serverSocket.accept()) {
                        socket.setSoTimeout(3000);
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)
                        );
                        Map<String, String> headers = new LinkedHashMap<>();
                        String line;
                        boolean firstLine = true;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            if (firstLine) {
                                firstLine = false;
                                continue;
                            }
                            int colon = line.indexOf(':');
                            if (colon > 0) {
                                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                                String value = line.substring(colon + 1).trim();
                                headers.put(name, value);
                            }
                        }
                        handshakeHeaders.set(headers);
                        headersCaptured.countDown();

                        OutputStream out = socket.getOutputStream();
                        out.write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                .getBytes(StandardCharsets.US_ASCII));
                        out.flush();
                    } catch (IOException ignored) {
                        headersCaptured.countDown();
                    }
                });

                SnapshotsWebSocketClient wsClient = new SnapshotsWebSocketClient(
                        HttpClient.newHttpClient(),
                        () -> URI.create("ws://127.0.0.1:" + port + "/core/mod-worker/snapshots/ws"),
                        () -> Optional.of("ws-token")
                );
                wsClient.connectAsync();

                assertTrue(headersCaptured.await(5, TimeUnit.SECONDS), "Expected WebSocket handshake request");
                Map<String, String> headers = handshakeHeaders.get();
                assertEquals("Bearer ws-token", headers.get("authorization"));
                assertEquals("26.3", headers.get(ModClientTelemetry.HEADER_GAME_VERSION.toLowerCase(Locale.ROOT)));
                assertEquals("1.0.0", headers.get(ModClientTelemetry.HEADER_MOD_VERSION.toLowerCase(Locale.ROOT)));
                assertEquals("neoforge", headers.get(ModClientTelemetry.HEADER_MOD_LOADER.toLowerCase(Locale.ROOT)));
                wsClient.close();
            } finally {
                acceptExecutor.shutdownNow();
            }
        }
    }

    @Test
    void updateCheckerServiceDoesNotSendBackendTelemetryHeaders() throws IOException {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("26.3", "1.0.0+26.3", "Fabric"));
        AtomicReference<Headers> receivedHeaders = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/update.json", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            byte[] bytes = "{\"homepage\":\"https://example.com\",\"promos\":{\"26.3-latest\":\"1.0.0\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/update.json");
        new UpdateCheckerService(uri).checkAsync("1.0.0+26.3", "26.3").join();

        Headers headers = receivedHeaders.get();
        assertNull(headers.getFirst(ModClientTelemetry.HEADER_GAME_VERSION));
        assertNull(headers.getFirst(ModClientTelemetry.HEADER_MOD_VERSION));
        assertNull(headers.getFirst(ModClientTelemetry.HEADER_MOD_LOADER));
    }

    @Test
    void allDomainBackendClientsUseHttpConfigBuildersForTelemetryHeaders() throws IOException {
        Path mainJavaDir = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(mainJavaDir), "Expected domain/src/main/java directory to exist");

        Set<String> allowedRawHttpRequestFiles = Set.of("HttpConfig.java", "UpdateCheckerService.java");
        Set<String> allowedRawWebSocketFiles = Set.of("HttpConfig.java");

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(mainJavaDir)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }

        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles) {
            String fileName = file.getFileName().toString();
            String content = Files.readString(file);
            if (!allowedRawHttpRequestFiles.contains(fileName) && content.contains("HttpRequest.newBuilder(")) {
                violations.add(mainJavaDir.relativize(file) + " calls HttpRequest.newBuilder(...) directly instead of HttpConfig.newBackendRequestBuilder(...)");
            }
            if (!allowedRawWebSocketFiles.contains(fileName) && content.contains(".newWebSocketBuilder(")) {
                violations.add(mainJavaDir.relativize(file) + " calls .newWebSocketBuilder(...) directly instead of HttpConfig.newBackendWebSocketBuilder(...)");
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Backend HTTP/WebSocket calls bypassing HttpConfig telemetry builders:\n" + String.join("\n", violations)
        );
    }
}
