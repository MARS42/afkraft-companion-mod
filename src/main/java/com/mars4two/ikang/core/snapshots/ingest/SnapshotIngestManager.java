package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.snapshots.*;
import com.mars4two.ikang.core.util.Conflater;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SnapshotIngestManager implements AutoCloseable {

    private static final SnapshotIngestManager INSTANCE = new SnapshotIngestManager();

    private static final Logger log = LoggerFactory.getLogger(SnapshotIngestManager.class);
    private final HttpClient client = HttpConfig.getHttpClient();

    private Conflater<SnapshotBase> conflater;
    private String worldName;

    public static SnapshotIngestManager getInstance() {
        return INSTANCE;
    }

    public void start(MinecraftServer minecraftServer) {
        worldName = minecraftServer.getWorldData().getLevelName();

        conflater = new Conflater<>(1000,
                SnapshotBase::key, SnapshotBase::changeToken,
                this::onEmit, this::onEmitError);
        conflater.start();
    }

    public void ingestContainerBlockEntity(Set<UUID> playerIds, BaseContainerBlockEntity containerBlockEntity) {
        conflater.submit(ContainerSnapshot.of(playerIds, containerBlockEntity));
    }

    public void ingestPlayerStats(PlayerStatsContainer player) {
        conflater.submit(PlayerStatsSnapshot.of(player));
        conflater.submit(PlayerEffectStatsSnapshot.of(player));
        conflater.submit(PlayerHandsStatsSnapshot.of(player));
    }

    private void onEmit(List<SnapshotBase> snapshots) {
        if(!AuthService.isAuthenticated()){
            return;
        }

        log.debug("Emitting {} snapshots...", snapshots.size());

        var request = new SnapshotsIngestRequest(worldName, snapshots.stream()
                .map(s -> new SnapshotsIngestRequest.SnapshotContent
                        (s.getSnapshotName(), s.playerIds(), GsonConfig.toJson(s.data())))
                .toList());

        var accessToken = AuthService.getAccessToken();

        if(accessToken.isEmpty()) {
            log.debug("Mod credentials not found");
            return;
        }

        try {
            String jsonBody = GsonConfig.toJson(request);
            log.debug("Sending snapshots ingest request: {}", jsonBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(HttpConfig.getSnapshotsIngestUri())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken.get())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }

            log.warn("Snapshots ingest response status: {}", response.statusCode());
        }catch (Exception e) {
            log.error("Error ingesting snapshots: {}", e.getMessage());
        }
    }

    private void onEmitError(Throwable error) {
        log.error("Error emitting snapshots", error);
    }

    @Override
    public void close() {
        try {
            conflater.close();
            conflater = null;
        } catch (Exception e) {
            log.error("Error closing SnapshotIngestManager", e);
        }
    }
}
