package com.mars4two.ikang.core.preferences;

import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.preferences.dtos.PreferencesOperationResult;
import com.mars4two.ikang.core.preferences.dtos.UpdateWorldPreferencesRequest;
import com.mars4two.ikang.core.preferences.dtos.WorldPreferencesResponse;
import com.mars4two.ikang.core.snapshots.SnapshotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Pure-Java HTTP client for {@code /core/mod-worker/preferences}.
 */
public class WorldPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(WorldPreferencesService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final BiFunction<UUID, UUID, URI> getUriProvider;
    private final Supplier<URI> putUriProvider;

    public WorldPreferencesService() {
        this(HttpConfig.getHttpClient(), HttpConfig::getModPreferencesUri, HttpConfig::getModPreferencesUri);
    }

    public WorldPreferencesService(
            HttpClient httpClient,
            BiFunction<UUID, UUID, URI> getUriProvider,
            Supplier<URI> putUriProvider
    ) {
        this.httpClient = httpClient;
        this.getUriProvider = getUriProvider;
        this.putUriProvider = putUriProvider;
    }

    public PreferencesOperationResult fetchPreferences(
            UUID serverId,
            UUID adminProfileId,
            String accessToken,
            Map<String, Boolean> fallbackSnapshots
    ) {
        if (serverId == null || adminProfileId == null || accessToken == null || accessToken.isBlank()) {
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NOT_AUTHENTICATED, fallbackSnapshots);
        }

        try {
            HttpRequest request = HttpConfig.newBackendRequestBuilder(getUriProvider.apply(serverId, adminProfileId))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response, fallbackSnapshots, "fetch");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while fetching world snapshot preferences", e);
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NETWORK_ERROR, fallbackSnapshots);
        } catch (Exception e) {
            log.warn("Failed to fetch world snapshot preferences: {}", e.getMessage());
            log.debug("Fetch world preferences error details", e);
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NETWORK_ERROR, fallbackSnapshots);
        }
    }

    public PreferencesOperationResult replacePreferences(
            UUID serverId,
            UUID adminProfileId,
            Map<String, Boolean> snapshots,
            String accessToken,
            Map<String, Boolean> fallbackSnapshots
    ) {
        if (serverId == null || adminProfileId == null || accessToken == null || accessToken.isBlank()) {
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NOT_AUTHENTICATED, fallbackSnapshots);
        }

        try {
            String jsonBody = GsonConfig.toJson(new UpdateWorldPreferencesRequest(serverId, adminProfileId, snapshots));
            HttpRequest request = HttpConfig.newBackendRequestBuilder(putUriProvider.get())
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response, fallbackSnapshots, "replace");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while replacing world snapshot preferences", e);
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NETWORK_ERROR, fallbackSnapshots);
        } catch (Exception e) {
            log.warn("Failed to replace world snapshot preferences: {}", e.getMessage());
            log.debug("Replace world preferences error details", e);
            return PreferencesOperationResult.failure(PreferencesOperationResult.Status.NETWORK_ERROR, fallbackSnapshots);
        }
    }

    private PreferencesOperationResult parseResponse(
            HttpResponse<String> response,
            Map<String, Boolean> fallbackSnapshots,
            String operation
    ) {
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            WorldPreferencesResponse dto = GsonConfig.fromJson(response.body(), WorldPreferencesResponse.class);
            Map<String, Boolean> rawMap = dto != null ? dto.snapshots() : Map.of();
            return PreferencesOperationResult.success(SnapshotType.mergeWithDefaults(rawMap));
        }

        PreferencesOperationResult.Status status = switch (code) {
            case 400 -> PreferencesOperationResult.Status.INVALID_REQUEST;
            case 401, 403 -> PreferencesOperationResult.Status.FORBIDDEN;
            case 404 -> PreferencesOperationResult.Status.NOT_FOUND;
            case 429 -> PreferencesOperationResult.Status.RATE_LIMITED;
            default -> PreferencesOperationResult.Status.NETWORK_ERROR;
        };
        log.warn("World preferences {} returned HTTP {} ({})", operation, code, status);
        return PreferencesOperationResult.failure(status, fallbackSnapshots);
    }
}
