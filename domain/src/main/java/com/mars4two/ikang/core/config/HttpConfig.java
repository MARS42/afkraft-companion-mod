package com.mars4two.ikang.core.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class HttpConfig {
    private static final String BACKEND_URL = "https://api-mvp.afkraft.com/core";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private static final AtomicReference<ModClientTelemetry> clientTelemetry =
            new AtomicReference<>(ModClientTelemetry.EMPTY);

    private HttpConfig() {
        // Utility class, no instances allowed
    }

    public static HttpClient getHttpClient() {
        return httpClient;
    }

    public static void configureClientTelemetry(ModClientTelemetry telemetry) {
        clientTelemetry.set(telemetry != null ? telemetry : ModClientTelemetry.EMPTY);
    }

    public static ModClientTelemetry getClientTelemetry() {
        return clientTelemetry.get();
    }

    public static HttpRequest.Builder newBackendRequestBuilder(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        clientTelemetry.get().applyTo(builder);
        return builder;
    }

    public static WebSocket.Builder newBackendWebSocketBuilder() {
        return newBackendWebSocketBuilder(httpClient);
    }

    public static WebSocket.Builder newBackendWebSocketBuilder(HttpClient client) {
        WebSocket.Builder builder = client.newWebSocketBuilder();
        clientTelemetry.get().applyTo(builder);
        return builder;
    }

    public static URI getServerIdUri() {
        return URI.create(BACKEND_URL + "/mod-auth/get-server");
    }

    public static URI getHasJoinedUri() {
        return URI.create(BACKEND_URL + "/mod-auth/has-joined");
    }

    public static URI getConnectUri() {
        return URI.create(BACKEND_URL + "/mod-auth/connect");
    }

    public static URI getCancelConnectUri() {
        return URI.create(BACKEND_URL + "/mod-auth/cancel-connect");
    }

    public static URI getHasConnectedUri() {
        return URI.create(BACKEND_URL + "/mod-auth/has-connected");
    }

    public static URI getIsConnectedUri(UUID profileId) {
        return URI.create(BACKEND_URL + "/mod-auth/is-connected" + "?profileId=" + profileId);
    }

    public static URI getSnapshotsIngestUri() {
        return URI.create(BACKEND_URL + "/mod-worker/ingest");
    }

    public static URI getSnapshotsWebSocketUri() {
        String wsBase = BACKEND_URL.replace("http://", "ws://").replace("https://", "wss://");
        return URI.create(wsBase + "/mod-worker/snapshots/ws");
    }

    public static URI getSnapshotsHeartbeatUri() {
        return URI.create(BACKEND_URL + "/mod-worker/snapshots/ws/heartbeat");
    }

    public static URI getModPreferencesUri(UUID serverId, UUID adminProfileId) {
        return URI.create(BACKEND_URL + "/mod-worker/preferences?serverId=" + serverId + "&adminProfileId=" + adminProfileId);
    }

    public static URI getModPreferencesUri() {
        return URI.create(BACKEND_URL + "/mod-worker/preferences");
    }

    public static URI getModPreferencesToggleUri() {
        return URI.create(BACKEND_URL + "/mod-worker/preferences/toggle");
    }

    public static URI getModCredentialsUri() {
        return URI.create(BACKEND_URL + "/mod-auth/get-credentials");
    }

    public static URI getRefreshAccessTokenUri() {
        return URI.create(BACKEND_URL + "/mod-auth/refresh-credentials");
    }

    public static URI getSendPlayerAuthorizationUri() {
        return URI.create(BACKEND_URL + "/mod-auth/authorize-join");
    }
}
