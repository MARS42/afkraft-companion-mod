package com.mars4two.ikang.client.core.worker;

import com.mars4two.ikang.client.core.auth.AuthService;
import com.mars4two.ikang.client.core.config.GsonConfig;
import com.mars4two.ikang.client.core.config.HttpConfig;
import com.mars4two.ikang.client.core.snapshots.PlayerStatsSnapshot;
import com.mars4two.ikang.core.snapshots.SnapshotBase;
import com.mars4two.ikang.client.core.worker.dtos.SnapshotsIngestRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AFKraftWorker {

    private static final Logger log = LoggerFactory.getLogger("AFKraft-Worker");

    private long lastExecution = 0;
    private static final long INTERVAL_MS = 1500;
    private final HttpClient client = HttpConfig.getHttpClient();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "afkraft-event-worker");
        t.setDaemon(true);
        return t;
    });

    public void onTick(Minecraft minecraft) {
        if (minecraft.player == null) return;
        if(minecraft.isPaused()) return;

        long now = System.currentTimeMillis();
        if (now - lastExecution < INTERVAL_MS) {
            return;
        }

        lastExecution = now;

        var player = minecraft.player;

        var playerStats = new PlayerStatsSnapshot(
                Optional.ofNullable(player.gameMode()).map(GameType::getName).orElse(""),
                player.getHealth(),
                player.getMaxHealth(),
                player.getFoodData().getFoodLevel(),
                player.getX(), player.getY(), player.getZ()
        );

        var snapshots = List.of(playerStats);

        worker.submit(() -> publishSnapshots(snapshots));
    }

    public void onClosed(Minecraft minecraft) {
        worker.shutdownNow();
    }

    private <T extends SnapshotBase> void publishSnapshots(List<T> snapshots) {
        var request = new SnapshotsIngestRequest(snapshots.stream()
                .map(SnapshotsIngestRequest.SnapshotContent::fromBase)
                .toList());
        log.info("Publishing snapshots: {}", request);

        AuthService.getAccessToken(accessToken -> {

            if(accessToken.isEmpty()) {
                log.warn("Mod credentials not found");
                return;
            }

            try {
                String jsonBody = GsonConfig.toJson(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(HttpConfig.getSnapshotsIngestUri())
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken.get())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                log.info("Snapshots ingest response: {}", response.body());
            }catch (Exception e) {
                log.error("Error ingesting snapshots: {}", e.getMessage());
            }
        });
    }
}
