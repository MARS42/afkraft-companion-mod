package com.mars4two.ikang.core.auth;

import com.mars4two.ikang.client.core.auth.ClientAuthDomainService;
import com.mars4two.ikang.client.core.auth.ClientConnectDomainService;
import com.mars4two.ikang.client.core.auth.dtos.ConnectResponse;
import com.mars4two.ikang.core.auth.dtos.HasConnectedResponse;
import com.mars4two.ikang.core.auth.dtos.ModCredentialsResponse;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.config.ModClientTelemetry;
import com.mars4two.ikang.core.snapshots.PlayerStatsSnapshot;
import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.ingest.SnapshotsIngestRequest;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffects;
import com.mars4two.ikang.core.snapshots.model.RawPlayerHands;
import com.mars4two.ikang.core.snapshots.model.RawPlayerStats;
import com.mars4two.ikang.core.util.PlayerStatsContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthIngestAndConnectDomainTest {

    private static final UUID SERVER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID PLAYER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @AfterEach
    void tearDown() {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.EMPTY);
        AuthService.clearModCredentials();
        ClientConnectDomainService.cancelActivePolling();
    }

    @Test
    void authServiceRequestsIncludeConfiguredClientTelemetryHeaders() {
        HttpConfig.configureClientTelemetry(ModClientTelemetry.of("26.3", "1.0.0+26.3", "Fabric"));

        var jsonPost = AuthService.buildJsonPostRequest(HttpConfig.getModCredentialsUri(), "{}");
        assertEquals(Optional.of("26.3"), jsonPost.headers().firstValue(ModClientTelemetry.HEADER_GAME_VERSION));
        assertEquals(Optional.of("1.0.0"), jsonPost.headers().firstValue(ModClientTelemetry.HEADER_MOD_VERSION));
        assertEquals(Optional.of("fabric"), jsonPost.headers().firstValue(ModClientTelemetry.HEADER_MOD_LOADER));
        assertEquals(Optional.of("application/json"), jsonPost.headers().firstValue("Content-Type"));

        var emptyPost = AuthService.buildEmptyPostRequest(HttpConfig.getIsConnectedUri(PLAYER_ID));
        assertEquals(Optional.of("26.3"), emptyPost.headers().firstValue(ModClientTelemetry.HEADER_GAME_VERSION));
        assertEquals(Optional.of("1.0.0"), emptyPost.headers().firstValue(ModClientTelemetry.HEADER_MOD_VERSION));
        assertEquals(Optional.of("fabric"), emptyPost.headers().firstValue(ModClientTelemetry.HEADER_MOD_LOADER));
    }

    @Test
    void credentialStoreSavesLoadsExpiresAndClearsCredentials() {
        ModCredentialsResponse validResponse = new ModCredentialsResponse(
                SERVER_ID,
                ADMIN_ID,
                new ModCredentialsResponse.TokenData("jwt-access", 300, 3600, "jwt-refresh", "Bearer", "mod")
        );

        CredentialStore.save(validResponse);
        assertEquals(Optional.of("jwt-access"), CredentialStore.getAccessToken());
        assertEquals(SERVER_ID, AuthService.getCurrentServerId());
        assertEquals(ADMIN_ID, AuthService.getAdminProfileId());
        assertTrue(AuthService.isAuthenticated());

        // Token within 30s expiration threshold immediately reports expired
        ModCredentialsResponse expiringResponse = new ModCredentialsResponse(
                SERVER_ID,
                ADMIN_ID,
                new ModCredentialsResponse.TokenData("short-lived", 10, 3600, "jwt-refresh", "Bearer", "mod")
        );
        CredentialStore.save(expiringResponse);
        assertEquals(Optional.empty(), CredentialStore.getAccessToken());

        AuthService.authorizePlayer(PLAYER_ID);
        assertTrue(AuthService.isPlayerAuthorized(PLAYER_ID));

        AuthService.clearModCredentials();
        assertEquals(Optional.empty(), CredentialStore.load());
    }

    @Test
    void snapshotIngestManagerBuildsExpectedWireRequestPayload() {
        PlayerStatsSnapshot snap = new PlayerStatsSnapshot(
                Set.of(PLAYER_ID),
                new RawPlayerStats("survival", 20.0f, 20.0f, 20, 300, 30, 0.5f, 1.0, 64.0, -2.0, 90.0f, 0.0f)
        );

        SnapshotsIngestRequest request = SnapshotIngestManager.buildIngestRequest(
                SERVER_ID,
                ADMIN_ID,
                List.of(snap)
        );

        assertEquals(SERVER_ID, request.serverId());
        assertEquals(ADMIN_ID, request.adminProfileId());
        assertEquals(Set.of(PLAYER_ID), request.participants());
        assertEquals(1, request.snapshots().size());
        assertEquals("playerStats", request.snapshots().get(0).name());

        String json = GsonConfig.toJson(request);
        assertTrue(json.contains("\"playerStats\""));
        assertTrue(json.contains("\"survival\""));
    }

    @Test
    void clientConnectDomainPollingHandlesApprovalExpirationAndCancellation() throws InterruptedException {
        // 1. Approval after retry
        CountDownLatch approvedLatch = new CountDownLatch(1);
        AtomicReference<String> receivedOtp = new AtomicReference<>();
        AtomicInteger attempts = new AtomicInteger(0);

        ClientConnectDomainService.startPollingWithPoller(
                new ConnectResponse("https://afkraft.com/v", "dev-1", 1, 30),
                10L,
                TimeUnit.MILLISECONDS,
                code -> attempts.incrementAndGet() >= 2
                        ? Optional.of(new HasConnectedResponse("otp-ok"))
                        : Optional.empty(),
                () -> {
                },
                resp -> {
                    receivedOtp.set(resp.otp());
                    approvedLatch.countDown();
                }
        );

        assertTrue(approvedLatch.await(2, TimeUnit.SECONDS));
        assertEquals("otp-ok", receivedOtp.get());

        // 2. Immediate expiration when expiresIn == 0
        CountDownLatch expiredLatch = new CountDownLatch(1);
        ClientConnectDomainService.startPollingWithPoller(
                new ConnectResponse("https://afkraft.com/v", "dev-2", 1, 0),
                10L,
                TimeUnit.MILLISECONDS,
                code -> Optional.empty(),
                expiredLatch::countDown,
                resp -> {
                }
        );
        assertTrue(expiredLatch.await(2, TimeUnit.SECONDS));

        // 3. Cancellation prevents poll execution
        AtomicInteger cancelledPolls = new AtomicInteger(0);
        Runnable cancelHandle = ClientConnectDomainService.startPollingWithPoller(
                new ConnectResponse("https://afkraft.com/v", "dev-3", 1, 30),
                50L,
                TimeUnit.MILLISECONDS,
                code -> {
                    cancelledPolls.incrementAndGet();
                    return Optional.empty();
                },
                () -> {
                },
                resp -> {
                }
        );
        cancelHandle.run();
        Thread.sleep(100L);
        assertEquals(0, cancelledPolls.get());

        // Verify SessionJoinPort contract compiles cleanly in pure domain
        ClientAuthDomainService.SessionJoinPort port = (id, token, srv) -> {
        };
        assertFalse(port == null);
    }

    @Test
    void snapshotIngestManagerSafelyNoOpsBeforeStartOrAfterClose() {
        SnapshotIngestManager manager = SnapshotIngestManager.getInstance();
        manager.close();

        PlayerStatsContainer stubPlayer = new PlayerStatsContainer() {
            @Override
            public UUID profileId() {
                return PLAYER_ID;
            }

            @Override
            public RawPlayerStats stats() {
                return new RawPlayerStats("survival", 20f, 20f, 20, 300, 1, 0f, 0, 64, 0, 0f, 0f);
            }

            @Override
            public RawPlayerEffects effects() {
                return new RawPlayerEffects(List.of());
            }

            @Override
            public RawPlayerHands handsStats() {
                return new RawPlayerHands(RawItemStack.EMPTY, RawItemStack.EMPTY);
            }

            @Override
            public RawPlayerAttackCadence attackCadence() {
                return RawPlayerAttackCadence.idle(12.5f);
            }

            @Override
            public com.mars4two.ikang.core.snapshots.model.RawMobSphere mobSphere() {
                return null;
            }
        };

        assertDoesNotThrow(() -> manager.ingestPlayerAttackCadence(stubPlayer));
        assertDoesNotThrow(() -> manager.ingestPlayerStats(stubPlayer));
    }
}
