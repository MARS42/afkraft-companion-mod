package com.mars4two.ikang.core.config;

import java.net.http.HttpRequest;
import java.net.http.WebSocket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable client environment telemetry sent to AFKraft Companion Core via HTTP and WebSocket headers.
 */
public record ModClientTelemetry(
        String gameVersion,
        String modVersion,
        String modLoader
) {

    public static final String HEADER_GAME_VERSION = "X-AFKraft-Game-Version";
    public static final String HEADER_MOD_VERSION = "X-AFKraft-Mod-Version";
    public static final String HEADER_MOD_LOADER = "X-AFKraft-Mod-Loader";

    private static final String UNKNOWN = "unknown";

    public static final ModClientTelemetry EMPTY = new ModClientTelemetry(null, null, null);

    public static ModClientTelemetry of(String rawGameVersion, String rawModVersion, String rawModLoader) {
        String normalizedGame = normalizeGameVersion(rawGameVersion);
        String normalizedMod = normalizeModVersion(rawModVersion);
        String normalizedLoader = normalizeModLoader(rawModLoader);
        if (normalizedGame == null && normalizedMod == null && normalizedLoader == null) {
            return EMPTY;
        }
        return new ModClientTelemetry(normalizedGame, normalizedMod, normalizedLoader);
    }

    public boolean hasAnyValue() {
        return gameVersion != null || modVersion != null || modLoader != null;
    }

    public void applyTo(HttpRequest.Builder builder) {
        if (builder == null) {
            return;
        }
        if (gameVersion != null) {
            builder.header(HEADER_GAME_VERSION, gameVersion);
        }
        if (modVersion != null) {
            builder.header(HEADER_MOD_VERSION, modVersion);
        }
        if (modLoader != null) {
            builder.header(HEADER_MOD_LOADER, modLoader);
        }
    }

    public void applyTo(WebSocket.Builder builder) {
        if (builder == null) {
            return;
        }
        if (gameVersion != null) {
            builder.header(HEADER_GAME_VERSION, gameVersion);
        }
        if (modVersion != null) {
            builder.header(HEADER_MOD_VERSION, modVersion);
        }
        if (modLoader != null) {
            builder.header(HEADER_MOD_LOADER, modLoader);
        }
    }

    public Map<String, String> toHeaders() {
        if (!hasAnyValue()) {
            return Map.of();
        }
        Map<String, String> headers = new LinkedHashMap<>(3);
        if (gameVersion != null) {
            headers.put(HEADER_GAME_VERSION, gameVersion);
        }
        if (modVersion != null) {
            headers.put(HEADER_MOD_VERSION, modVersion);
        }
        if (modLoader != null) {
            headers.put(HEADER_MOD_LOADER, modLoader);
        }
        return Collections.unmodifiableMap(headers);
    }

    static String normalizeGameVersion(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || UNKNOWN.equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    static String normalizeModVersion(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || UNKNOWN.equalsIgnoreCase(trimmed)) {
            return null;
        }
        int buildMetaIndex = trimmed.indexOf('+');
        if (buildMetaIndex > 0) {
            trimmed = trimmed.substring(0, buildMetaIndex).trim();
        }
        if (trimmed.isEmpty() || UNKNOWN.equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    static String normalizeModLoader(String raw) {
        if (raw == null) {
            return null;
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty() || UNKNOWN.equals(lower)) {
            return null;
        }
        return lower;
    }
}
