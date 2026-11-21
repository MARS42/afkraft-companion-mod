package com.mars4two.ikang.core.auth;

import com.mars4two.ikang.core.auth.dtos.*;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-service");
    private static final HttpClient client = HttpConfig.getHttpClient();

    private static final Set<UUID> authorizedPlayers = ConcurrentHashMap.newKeySet();

    private static final AtomicReference<String> credentialsOwner = new AtomicReference<>();

    private AuthService() {
        // Utility class, no instances allowed
    }

    public static String getServerId(UUID profileId) throws AuthenticationException {
        try {
            String jsonBody = GsonConfig.toJson(new GetServerIdRequest(profileId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getServerIdUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthenticationException("No se pudo obtener serverId (HTTP " + response.statusCode() + ")");
            }

            GetServerIdResponse responseDto = GsonConfig.fromJson(response.body(), GetServerIdResponse.class);
            return responseDto.serverId();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Error de conexión con el backend: " + e.getMessage());
        }
    }

    public static HasJoinedResponse validateHasJoined(String username, String serverId, UUID profileId)
            throws AuthenticationException {
        try {
            String jsonBody = GsonConfig.toJson(new HasJoinedRequest(username, serverId, profileId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getHasJoinedUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new AuthenticationException("Validación fallida: credenciales inválidas");
            }

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            HasJoinedResponse dto = GsonConfig.fromJson(response.body(), HasJoinedResponse.class);
            log.info("Has joined success");
            return dto;
        } catch (Exception e) {
            log.error("Unexpected error during hasJoined validation: ", e);
            throw new AuthenticationException(e.getMessage());
        }
    }

    public static void authenticate(
            String profileId,
            String serverId,
            String joinOtp,
            String serverName,
            String ownerUsername,
            Runnable successCallback,
            Runnable errorCallback
    ) {
        log.info("Authenticating mod by user {}...", ownerUsername);
        CompletableFuture.runAsync(() -> {
            var modCredentials = getModCredentials(profileId, serverId, joinOtp, serverName);

            if (modCredentials.isEmpty()) {
                log.warn("Unable to authenticate user");
                errorCallback.run();
                return;
            }

            CredentialStore.save(modCredentials.get());
            credentialsOwner.set(ownerUsername);
            successCallback.run();
        });
    }

    public static boolean isAuthenticated() {
        return getAccessToken().isPresent();
    }

    public static String getCredentialsOwner() {
        return credentialsOwner.get();
    }

    public static UUID getCurrentServerId() {
        return CredentialStore.getServerId();
    }

    public static Optional<UUID> findCurrentServerId() {
        return CredentialStore.findServerId();
    }

    public static UUID getAdminProfileId() {
        return CredentialStore.getAdminProfileId();
    }

    public static Optional<UUID> findAdminProfileId() {
        return CredentialStore.findAdminProfileId();
    }

    public static Optional<String> getAccessToken() {
        var modCredentials = CredentialStore.load();
        if (modCredentials.isEmpty()) {
            return Optional.empty();
        }

        var currentAccessToken = CredentialStore.getAccessToken();
        if (currentAccessToken.isPresent()) {
            log.debug("Access token is present");
            return currentAccessToken;
        }

        log.info("Access token is not present or expired, refreshing with mod credentials...");
        var refreshedCredentials = refreshAccessToken(modCredentials.get());

        if (refreshedCredentials.isEmpty()) {
            return Optional.empty();
        }

        CredentialStore.save(refreshedCredentials.get());

        log.info("Access token refreshed successfully");
        return CredentialStore.getAccessToken();
    }

    public static boolean getIsConnected(UUID profileId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getIsConnectedUri(profileId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() != 200) {
                log.warn("Is connected is not success (HTTP {}, Body: {})", response.statusCode(), body);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during hasConnected validation: ", e);
            return false;
        }
    }

    public static void checkIfIsConnected(UUID profileId, Consumer<Boolean> callback) {
        log.info("Checking if profile {} is connected...", profileId);
        CompletableFuture.runAsync(() -> {
            var isConnected = getIsConnected(profileId);
            if (!isConnected) {
                log.info("User is not connected");
                callback.accept(false);
                return;
            }
            log.info("User is connected");
            callback.accept(true);
        });
    }

    private static Optional<ModCredentialsResponse> getModCredentials(String profileId, String serverId, String joinOtp, String serverName) {
        log.info("Getting mod credentials...");

        try {
            String jsonBody = GsonConfig.toJson(new GetModCredentialsRequest(profileId, serverId, joinOtp, serverName));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getModCredentialsUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            ModCredentialsResponse dto = GsonConfig.fromJson(response.body(), ModCredentialsResponse.class);

            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.error("Unexpected error during getting mod credentials: ", e);
            return Optional.empty();
        }
    }

    private static Optional<ModCredentialsResponse> refreshAccessToken(ModCredentials modCredentials) {
        log.info("Refreshing access token...");

        try {
            String jsonBody = GsonConfig.toJson(new RefreshModCredentialsRequest(modCredentials.refreshToken(), modCredentials.serverId(), modCredentials.adminProfileId()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(HttpConfig.getRefreshAccessTokenUri())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new AuthenticationException("Validación fallida: credenciales inválidas");
            }

            if (response.statusCode() != 200) {
                throw new AuthenticationException("Error en validación (HTTP " + response.statusCode() + ")");
            }

            ModCredentialsResponse dto = GsonConfig.fromJson(response.body(), ModCredentialsResponse.class);

            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.error("Unexpected error during refreshing mod credentials: ", e);
            return Optional.empty();
        }
    }

    public static void clearModCredentials() {
        CredentialStore.clear();
    }

    public static void authorizePlayer(UUID profileId) {
        log.info("Adding player to authorized players list");
        authorizedPlayers.add(profileId);
        log.info("Player added to authorized players list");
    }

    public static boolean isPlayerAuthorized(UUID profileId) {
        return authorizedPlayers.contains(profileId);
    }
}
