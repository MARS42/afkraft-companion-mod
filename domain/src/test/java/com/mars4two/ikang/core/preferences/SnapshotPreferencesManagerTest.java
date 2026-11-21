package com.mars4two.ikang.core.preferences;

import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.preferences.dtos.PreferencesOperationResult;
import com.mars4two.ikang.core.preferences.dtos.WorldPreferencesResponse;
import com.mars4two.ikang.core.snapshots.SnapshotType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotPreferencesManagerTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void defaultsEnableAllKnownSnapshotsAndMergeSparseMaps() {
        Map<String, Boolean> defaults = SnapshotType.defaultPreferencesMap();
        assertEquals(SnapshotType.values().length, defaults.size());
        defaults.values().forEach(enabled -> assertTrue(enabled));

        Map<String, Boolean> sparse = Map.of(
                "playerMobSphere", false,
                "load-station", false,
                "unknownLegacyKey", false
        );
        Map<String, Boolean> merged = SnapshotType.mergeWithDefaults(sparse);
        assertEquals(SnapshotType.values().length, merged.size());
        assertFalse(merged.get("playerMobSphere"));
        assertFalse(merged.get("load-station"));
        assertTrue(merged.get("playerStats"));
        assertFalse(merged.containsKey("unknownLegacyKey"));
    }

    @Test
    void gsonDeserializesBackendIsoInstantUpdatedAtCleanly() {
        String json = """
                {
                  "worldId": "11111111-1111-1111-1111-111111111111",
                  "serverId": "22222222-2222-2222-2222-222222222222",
                  "editable": true,
                  "snapshots": {"playerMobSphere": false, "container": true},
                  "updatedAt": "2026-11-20T05:15:00Z"
                }
                """;
        WorldPreferencesResponse parsed = GsonConfig.fromJson(json, WorldPreferencesResponse.class);
        assertTrue(parsed.editable());
        assertEquals("2026-11-20T05:15:00Z", parsed.updatedAt());
        assertEquals(Boolean.FALSE, parsed.snapshots().get("playerMobSphere"));
    }

    @Test
    void fetchAndReplaceSynchronizeWithBackendAndNotifyChangedGroups() throws IOException {
        UUID serverId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        AtomicReference<String> lastAuthHeader = new AtomicReference<>();
        AtomicReference<String> lastPutBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/core/mod-worker/preferences", exchange -> {
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if ("GET".equals(exchange.getRequestMethod())) {
                String body = GsonConfig.toJson(new WorldPreferencesResponse(
                        worldId, serverId, true, Map.of("playerMobSphere", false), "2026-11-20T06:00:00Z"
                ));
                sendJson(exchange, 200, body);
                return;
            }
            if ("PUT".equals(exchange.getRequestMethod())) {
                String reqBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                lastPutBody.set(reqBody);
                String body = GsonConfig.toJson(new WorldPreferencesResponse(
                        worldId, serverId, true, Map.of("playerMobSphere", true, "container", false), "2026-11-20T06:01:00Z"
                ));
                sendJson(exchange, 200, body);
                return;
            }
            sendJson(exchange, 405, "");
        });
        server.start();

        int port = server.getAddress().getPort();
        WorldPreferencesService remote = new WorldPreferencesService(
                HttpClient.newHttpClient(),
                (sId, aId) -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences?serverId=" + sId + "&adminProfileId=" + aId),
                () -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences")
        );

        SnapshotPreferencesManager manager = new SnapshotPreferencesManager(
                remote,
                () -> Optional.of(new SnapshotPreferencesManager.AuthContext(serverId, adminId, "secret-token"))
        );

        List<Set<String>> notifications = new CopyOnWriteArrayList<>();
        manager.addChangeListener(notifications::add);

        PreferencesOperationResult fetched = manager.refreshFromBackend();
        assertTrue(fetched.isSuccess());
        assertEquals("Bearer secret-token", lastAuthHeader.get());
        assertFalse(manager.isEnabled(SnapshotType.PLAYER_MOB_SPHERE));
        assertTrue(manager.isEnabled(SnapshotType.CONTAINER));
        assertEquals(List.of(Set.of("playerMobSphere")), notifications);

        Map<String, Boolean> update = new LinkedHashMap<>(manager.getEffectivePreferences());
        update.put("playerMobSphere", true);
        update.put("container", false);

        PreferencesOperationResult saved = manager.saveToBackendAsync(update).join();
        assertTrue(saved.isSuccess());
        assertTrue(lastPutBody.get().contains(serverId.toString()));
        assertTrue(lastPutBody.get().contains(adminId.toString()));
        assertTrue(manager.isEnabled(SnapshotType.PLAYER_MOB_SPHERE));
        assertFalse(manager.isEnabled(SnapshotType.CONTAINER));
        assertEquals(Set.of("playerMobSphere", "container"), notifications.get(1));

        manager.reset();
        assertTrue(manager.isEnabled(SnapshotType.CONTAINER));
        assertEquals(Set.of("container"), notifications.get(2));
    }

    @Test
    void failedSavePreservesActiveStateAndRejectsUnknownSnapshotKeysLocally() throws IOException {
        UUID serverId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AtomicInteger putCalls = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/core/mod-worker/preferences", exchange -> {
            putCalls.incrementAndGet();
            sendJson(exchange, 429, "");
        });
        server.start();

        int port = server.getAddress().getPort();
        WorldPreferencesService remote = new WorldPreferencesService(
                HttpClient.newHttpClient(),
                (sId, aId) -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences"),
                () -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences")
        );

        SnapshotPreferencesManager manager = new SnapshotPreferencesManager(
                remote,
                () -> Optional.of(new SnapshotPreferencesManager.AuthContext(serverId, adminId, "token"))
        );

        // Unknown key must be rejected before any HTTP call
        PreferencesOperationResult invalid = manager.saveToBackendAsync(Map.of("badKey", false)).join();
        assertEquals(PreferencesOperationResult.Status.INVALID_REQUEST, invalid.status());
        assertEquals(0, putCalls.get());

        // 429 must preserve active state
        PreferencesOperationResult rateLimited = manager.saveToBackendAsync(Map.of("playerStats", false)).join();
        assertEquals(PreferencesOperationResult.Status.RATE_LIMITED, rateLimited.status());
        assertEquals(1, putCalls.get());
        assertTrue(manager.isEnabled(SnapshotType.PLAYER_STATS));
    }

    @Test
    void concurrentSaveIsRejectedWhileFirstSaveIsInFlight() throws Exception {
        UUID serverId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        CountDownLatch requestEntered = new CountDownLatch(1);
        CountDownLatch allowRelease = new CountDownLatch(1);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/core/mod-worker/preferences", exchange -> {
            requestEntered.countDown();
            try {
                allowRelease.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendJson(exchange, 200, GsonConfig.toJson(new WorldPreferencesResponse(
                    UUID.randomUUID(), serverId, true, Map.of("playerStats", false), "2026-11-20T06:00:00Z"
            )));
        });
        server.start();

        int port = server.getAddress().getPort();
        WorldPreferencesService remote = new WorldPreferencesService(
                HttpClient.newHttpClient(),
                (sId, aId) -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences"),
                () -> URI.create("http://127.0.0.1:" + port + "/core/mod-worker/preferences")
        );
        SnapshotPreferencesManager manager = new SnapshotPreferencesManager(
                remote,
                () -> Optional.of(new SnapshotPreferencesManager.AuthContext(serverId, adminId, "token"))
        );

        var first = manager.saveToBackendAsync(Map.of("playerStats", false));
        assertTrue(requestEntered.await(5, TimeUnit.SECONDS));

        var second = manager.saveToBackendAsync(Map.of("container", false)).join();
        assertEquals(PreferencesOperationResult.Status.SAVE_IN_PROGRESS, second.status());

        allowRelease.countDown();
        assertTrue(first.join().isSuccess());
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
