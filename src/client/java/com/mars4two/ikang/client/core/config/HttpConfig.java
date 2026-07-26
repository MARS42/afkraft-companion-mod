package com.mars4two.ikang.client.core.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

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

    public static URI getHasConnectedUri() {
        return URI.create(BACKEND_URL + "/mod-auth/has-connected");
    }
}
