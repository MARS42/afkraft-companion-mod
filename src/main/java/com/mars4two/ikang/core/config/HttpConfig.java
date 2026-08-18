package com.mars4two.ikang.core.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

public class HttpConfig {
    private static final String BACKEND_URL = "https://api-mvp.afkraft.com/core";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    private HttpConfig() {
        // Utility class, no instances allowed
    }

    public static HttpClient getHttpClient() {
        return httpClient;
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

    public static URI getHasConnectedUri() {
        return URI.create(BACKEND_URL + "/mod-auth/has-connected");
    }
    public static URI getIsConnectedUri(UUID profileId) {
        return URI.create(BACKEND_URL + "/mod-auth/is-connected" + "?profileId=" + profileId);
    }

    public static URI getSnapshotsIngestUri() {
        return URI.create(BACKEND_URL + "/mod-worker/ingest");
    }

    public static URI getModCredentialsUri() {
        return URI.create(BACKEND_URL + "/mod-auth/get-credentials");
    }

    public static URI getRefreshAccessTokenUri() {
        return URI.create(BACKEND_URL + "/mod-auth/refresh-credentials");
    }
}
