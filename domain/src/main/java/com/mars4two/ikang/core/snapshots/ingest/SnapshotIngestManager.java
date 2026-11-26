package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.preferences.SnapshotPreferencesManager;
import com.mars4two.ikang.core.snapshots.*;
import com.mars4two.ikang.core.util.Conflater;
import com.mars4two.ikang.core.util.ContainerView;
import com.mars4two.ikang.core.util.InventoryView;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnapshotIngestManager implements AutoCloseable {

    private static final SnapshotIngestManager INSTANCE = new SnapshotIngestManager();

    private static final Logger log = LoggerFactory.getLogger(SnapshotIngestManager.class);
    private final HttpClient client = HttpConfig.getHttpClient();
    private final SnapshotsWebSocketClient wsClient = new SnapshotsWebSocketClient(client);
    private final SnapshotPreferencesManager preferencesManager;

    private static final long FLUSH_PERIOD_MILLIS = 1000;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 30_000;
    private static final long HEARTBEAT_IDLE_THRESHOLD_MILLIS = 30_000;

    private volatile Conflater<SnapshotBase> conflater;
    private volatile String worldName;

    public SnapshotIngestManager() {
        this(SnapshotPreferencesManager.getInstance());
    }

    public SnapshotIngestManager(SnapshotPreferencesManager preferencesManager) {
        this.preferencesManager = Objects.requireNonNull(preferencesManager, "preferencesManager");
        this.preferencesManager.addChangeListener(this::onPreferencesChanged);
    }

    public static SnapshotIngestManager getInstance() {
        return INSTANCE;
    }

    private void onPreferencesChanged(Set<String> changedSnapshotNames) {
        Conflater<SnapshotBase> active = this.conflater;
        if (active != null && changedSnapshotNames != null && !changedSnapshotNames.isEmpty()) {
            active.invalidateGroups(changedSnapshotNames);
        }
    }

    public synchronized void start(String worldName) {
        if (this.conflater != null) {
            try {
                this.conflater.close();
            } catch (Exception e) {
                log.warn("Error closing previous conflater before restart", e);
            }
        }
        this.worldName = worldName;

        wsClient.connectAsync();

        Conflater<SnapshotBase> nextConflater = new Conflater<>(
                FLUSH_PERIOD_MILLIS,
                SnapshotBase::key,
                SnapshotBase::changeToken,
                SnapshotBase::getSnapshotName,
                this::onEmit,
                this::onEmitError,
                HEARTBEAT_INTERVAL_MILLIS,
                HEARTBEAT_IDLE_THRESHOLD_MILLIS,
                this::onHeartbeat
        );
        nextConflater.start();
        this.conflater = nextConflater;
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean isSnapshotEnabled(SnapshotType type) {
        return preferencesManager.isEnabled(type);
    }

    private void submitSnapshot(SnapshotBase snapshot) {
        Conflater<SnapshotBase> active = this.conflater;
        if (active != null && preferencesManager.isEnabled(snapshot.getSnapshotName())) {
            active.submit(snapshot);
        }
    }

    public void ingestContainerBlockEntity(Set<UUID> playerIds, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.CONTAINER)) {
            return;
        }
        submitSnapshot(ContainerSnapshot.of(playerIds, containerView));
    }

    public void ingestContainerDeleted(Set<UUID> playerIds, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.CONTAINER)) {
            return;
        }
        submitSnapshot(ContainerSnapshot.ofDeleted(playerIds, containerView));
    }

    public void ingestPortableContainerBlockEntity(Set<UUID> playerIds, UUID containerId, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.CONTAINER)) {
            return;
        }
        submitSnapshot(ContainerSnapshot.ofPortable(playerIds, containerId, containerView));
    }

    public void ingestPortableContainerDeleted(Set<UUID> playerIds, UUID containerId, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.CONTAINER)) {
            return;
        }
        submitSnapshot(ContainerSnapshot.ofPortableDeleted(playerIds, containerId, containerView));
    }

    public void ingestLoadStationBlockEntity(Set<UUID> playerIds, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.LOAD_STATION)) {
            return;
        }
        submitSnapshot(LoadStationSnapshot.of(playerIds, containerView));
    }

    public void ingestLoadStationDeleted(Set<UUID> playerIds, ContainerView containerView) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.LOAD_STATION)) {
            return;
        }
        submitSnapshot(LoadStationSnapshot.ofDeleted(playerIds, containerView));
    }

    public void ingestPlayerStats(PlayerStatsContainer player) {
        if (this.conflater == null) {
            return;
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_STATS)) {
            submitSnapshot(PlayerStatsSnapshot.of(player));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_EFFECT_STATS)) {
            submitSnapshot(PlayerEffectStatsSnapshot.of(player));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_HANDS_STATS)) {
            submitSnapshot(PlayerHandsStatsSnapshot.of(player));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_ATTACK_CADENCE)) {
            submitSnapshot(PlayerAttackCadenceSnapshot.of(player));
        }
    }

    public void ingestPlayerAttackCadence(PlayerStatsContainer player) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.PLAYER_ATTACK_CADENCE)) {
            return;
        }
        submitSnapshot(PlayerAttackCadenceSnapshot.of(player));
    }

    public void ingestPlayerMobSphere(PlayerStatsContainer player) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.PLAYER_MOB_SPHERE)) {
            return;
        }
        submitSnapshot(PlayerMobSphereSnapshot.of(player));
    }

    public void ingestPlayerEquipmentState(InventoryView playerInventory) {
        if (this.conflater == null || !preferencesManager.isEnabled(SnapshotType.PLAYER_EQUIPMENT)) {
            return;
        }
        submitSnapshot(PlayerEquipmentSnapshot.of(playerInventory));
    }

    public void ingestPlayerFullInventoryState(InventoryView playerInventory) {
        if (this.conflater == null) {
            return;
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_HOTBAR)) {
            submitSnapshot(PlayerHotbarSnapshot.of(playerInventory));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_INVENTORY)) {
            submitSnapshot(PlayerInventorySnapshot.of(playerInventory));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_EQUIPMENT)) {
            submitSnapshot(PlayerEquipmentSnapshot.of(playerInventory));
        }
    }

    public void ingestPlayerInventoryState(InventoryView playerInventory) {
        if (this.conflater == null) {
            return;
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_INVENTORY)) {
            submitSnapshot(PlayerInventorySnapshot.of(playerInventory));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_EQUIPMENT)) {
            submitSnapshot(PlayerEquipmentSnapshot.of(playerInventory));
        }
    }

    public void ingestPlayerHotbarState(InventoryView playerInventory) {
        if (this.conflater == null) {
            return;
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_HOTBAR)) {
            submitSnapshot(PlayerHotbarSnapshot.of(playerInventory));
        }
        if (preferencesManager.isEnabled(SnapshotType.PLAYER_EQUIPMENT)) {
            submitSnapshot(PlayerEquipmentSnapshot.of(playerInventory));
        }
    }

    /**
     * Package-private hook allowing unit tests to inject a deterministic Conflater without opening WebSockets.
     */
    synchronized void attachConflaterForTest(Conflater<SnapshotBase> testConflater) {
        this.conflater = testConflater;
    }

    public static SnapshotsIngestRequest buildIngestRequest(
            UUID serverId,
            UUID adminProfileId,
            List<SnapshotBase> snapshots
    ) {
        var snapshotContent = snapshots.stream()
                .map(s -> new SnapshotsIngestRequest.SnapshotContent(
                        s.getSnapshotName(),
                        s.playerIds(),
                        s.data().toPayload()
                ))
                .toList();
        var participants = snapshots.stream()
                .flatMap(s -> s.playerIds().stream())
                .collect(Collectors.toSet());
        return new SnapshotsIngestRequest(serverId, adminProfileId, participants, snapshotContent);
    }

    private void onEmit(List<SnapshotBase> snapshots) {
        if (!AuthService.isAuthenticated()) {
            return;
        }

        List<SnapshotBase> enabledSnapshots = snapshots.stream()
                .filter(s -> preferencesManager.isEnabled(s.getSnapshotName()))
                .toList();
        if (enabledSnapshots.isEmpty()) {
            return;
        }

        log.debug("Emitting {} snapshots...", enabledSnapshots.size());

        var accessToken = AuthService.getAccessToken();
        if (accessToken.isEmpty()) {
            log.debug("Unable to emit snapshots");
            return;
        }

        try {
            var serverId = AuthService.getCurrentServerId();
            var adminProfileId = AuthService.getAdminProfileId();

            var request = buildIngestRequest(serverId, adminProfileId, enabledSnapshots);

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
            HttpRequest httpRequest = buildAuthorizedJsonPostRequest(
                    HttpConfig.getSnapshotsIngestUri(),
                    accessToken.get(),
                    jsonBody
            );

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

            HttpRequest httpRequest = buildAuthorizedJsonPostRequest(
                    HttpConfig.getSnapshotsHeartbeatUri(),
                    accessToken.get(),
                    jsonBody
            );

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

    static HttpRequest buildAuthorizedJsonPostRequest(URI uri, String accessToken, String jsonBody) {
        return HttpConfig.newBackendRequestBuilder(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    @Override
    public synchronized void close() {
        try {
            wsClient.close();
        } catch (Exception e) {
            log.error("Error closing SnapshotsWebSocketClient", e);
        }
        try {
            Conflater<SnapshotBase> active = this.conflater;
            this.conflater = null;
            if (active != null) {
                active.close();
            }
        } catch (Exception e) {
            log.error("Error closing SnapshotIngestManager", e);
        }
    }
}
