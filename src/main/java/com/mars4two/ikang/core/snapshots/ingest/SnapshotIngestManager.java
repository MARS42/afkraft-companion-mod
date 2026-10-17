package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.snapshots.*;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import com.mars4two.ikang.core.util.Conflater;
import com.mars4two.ikang.core.util.PlayerInventoryWrapper;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnapshotIngestManager implements AutoCloseable {

    private static final SnapshotIngestManager INSTANCE = new SnapshotIngestManager();

    private static final Logger log = LoggerFactory.getLogger(SnapshotIngestManager.class);
    private final HttpClient client = HttpConfig.getHttpClient();
    private final SnapshotsWebSocketClient wsClient = new SnapshotsWebSocketClient(client);

    private static final long FLUSH_PERIOD_MILLIS = 1000;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 30_000;
    private static final long HEARTBEAT_IDLE_THRESHOLD_MILLIS = 30_000;

    private Conflater<SnapshotBase> conflater;
    private String worldName;

    public static SnapshotIngestManager getInstance() {
        return INSTANCE;
    }

    public void start(MinecraftServer minecraftServer) {
        worldName = minecraftServer.getWorldData().getLevelName();

        wsClient.connectAsync();

        conflater = new Conflater<>(
                FLUSH_PERIOD_MILLIS,
                SnapshotBase::key,
                SnapshotBase::changeToken,
                this::onEmit,
                this::onEmitError,
                HEARTBEAT_INTERVAL_MILLIS,
                HEARTBEAT_IDLE_THRESHOLD_MILLIS,
                this::onHeartbeat
        );
        conflater.start();
    }

    public void ingestContainerBlockEntity(Set<UUID> playerIds, BlockEntityWrapper blockEntityWrapper) {
        conflater.submit(ContainerSnapshot.of(playerIds, blockEntityWrapper));
    }

    public void ingestPlayerStats(PlayerStatsContainer player) {
        conflater.submit(PlayerStatsSnapshot.of(player));
        conflater.submit(PlayerEffectStatsSnapshot.of(player));
        conflater.submit(PlayerHandsStatsSnapshot.of(player));
        conflater.submit(PlayerAttackCadenceSnapshot.of(player));
    }

    public void ingestPlayerAttackCadence(PlayerStatsContainer player) {
        conflater.submit(PlayerAttackCadenceSnapshot.of(player));
    }

    public void ingestPlayerMobSphere(PlayerStatsContainer player) {
        conflater.submit(PlayerMobSphereSnapshot.of(player));
    }

    public void ingestPlayerInventoryState(PlayerInventoryWrapper playerInventory) {
        conflater.submit(PlayerInventorySnapshot.of(playerInventory));
        conflater.submit(PlayerEquipmentSnapshot.of(playerInventory));
    }

    public void ingestPlayerHotbarState(PlayerInventoryWrapper playerInventory) {
        conflater.submit(PlayerHotbarSnapshot.of(playerInventory));
        conflater.submit(PlayerEquipmentSnapshot.of(playerInventory));
    }

    private void onEmit(List<SnapshotBase> snapshots) {
        if (!AuthService.isAuthenticated()) {
            return;
        }

        log.debug("Emitting {} snapshots...", snapshots.size());

        var accessToken = AuthService.getAccessToken();
        if (accessToken.isEmpty()) {
            log.debug("Unable to emit snapshots");
            return;
        }

        try {
            var serverId = AuthService.getCurrentServerId();
            var adminProfileId = AuthService.getAdminProfileId();

            var snapshotContent = snapshots.stream()
                    .map(s -> new SnapshotsIngestRequest.SnapshotContent
                            (s.getSnapshotName(), s.playerIds(), s.data().toPayload()))
                    .toList();
            var participants = snapshots.stream().flatMap(s -> s.playerIds().stream()).collect(Collectors.toSet());

            var request = new SnapshotsIngestRequest(serverId, adminProfileId, participants, snapshotContent);

            String jsonBody = GsonConfig.toJson(request);
            log.debug("Sending snapshots ingest request: {}", jsonBody);

            // Attempt non-blocking delivery via persistent WebSocket
            boolean sentViaWs = wsClient.send(jsonBody);
            if (sentViaWs) {
                log.debug("Snapshots emitted via WebSocket");
                return;
            }

            // Fallback to HTTP POST if WebSocket is disconnected
            log.debug("WebSocket not connected; falling back to HTTP POST");
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(HttpConfig.getSnapshotsIngestUri())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken.get())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }

            log.warn("Snapshots ingest response status: {}", response.statusCode());
        } catch (Exception e) {
            log.error("Error ingesting snapshots: {}", e.getMessage());
            log.debug("Error details:", e);
        }
    }

    private void onEmitError(Throwable error) {
        log.error("Error emitting snapshots", error);
    }

    private void onHeartbeat() {
        if (!AuthService.isAuthenticated()) {
            return;
        }

        log.debug("Emitting heartbeat...");

        var accessToken = AuthService.getAccessToken();
        if (accessToken.isEmpty()) {
            log.debug("Unable to emit heartbeat: access token missing");
            return;
        }

        try {
            var serverId = AuthService.getCurrentServerId();
            var adminProfileId = AuthService.getAdminProfileId();
            if (serverId == null || adminProfileId == null) {
                log.debug("Unable to emit heartbeat: serverId or adminProfileId is missing");
                return;
            }

            var request = new HeartbeatRequest(serverId, adminProfileId);
            String jsonBody = GsonConfig.toJson(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(HttpConfig.getSnapshotsHeartbeatUri())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken.get())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.debug("Heartbeat emitted successfully");
                        } else {
                            log.warn("Heartbeat response status: {}", response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error emitting heartbeat: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error preparing heartbeat request: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            wsClient.close();
        } catch (Exception e) {
            log.error("Error closing SnapshotsWebSocketClient", e);
        }
        try {
            if (conflater != null) {
                conflater.close();
                conflater = null;
            }
        } catch (Exception e) {
            log.error("Error closing SnapshotIngestManager", e);
        }
    }
}
