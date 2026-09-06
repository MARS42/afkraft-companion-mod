package com.mars4two.ikang.core.auth;

import com.mars4two.ikang.core.auth.dtos.*;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-service");
    private static final HttpClient client = HttpConfig.getHttpClient();

    private static final AtomicBoolean isConnected = new AtomicBoolean(false);

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
        }  catch (Exception e) {
            log.error("Unexpected error during hasJoined validation: ", e);
            throw new AuthenticationException(e.getMessage());
        }
    }

    public static void checkIfIsConnected(UUID profileId, Consumer<Boolean> callback) {
        log.info("Checking if profile {} is connected...", profileId);
        CompletableFuture.runAsync(() -> {
            var isConnected = getIsConnected(profileId);
            AuthService.isConnected.set(isConnected);
            if(!isConnected) {
                log.info("User is not connected");
                callback.accept(false);
                return;
            }
            log.info("User is connected");
            callback.accept(true);
        });
    }

    public static void authenticate(GetCredentialsRequestServerboundPayload payload, ServerPlayer player) {
        log.info("Authenticating mod by user {}...", player.getGameProfile().name());
        CompletableFuture.runAsync(() -> {

            var modCredentials = getModCredentials(payload.profileId(), payload.serverId(), payload.joinOtp());

            if(modCredentials.isEmpty()) {
                log.warn("Unable to authenticate user");
                player.sendSystemMessage(Component.literal("Error al generar credenciales AFKraft"));
                return;
            }

            player.sendSystemMessage(Component.literal("Credenciales AFKraft generadas correctamente"));
            CredentialStore.save(modCredentials.get());
        });
    }

    public static boolean isAuthenticated() {
        return getAccessToken().isPresent();
    }

    public static Optional<String> getAccessToken() {
        var modCredentials = CredentialStore.load();
        if(modCredentials.isEmpty()) {
            log.info("No mod credentials found");
            return Optional.empty();
        }

        var currentAccessToken = CredentialStore.getAccessToken();
        if(currentAccessToken.isPresent()) {
            log.debug("Access token is present");
            return currentAccessToken;
        }

        log.info("Access token is not present or expired, refreshing with mod credentials...");
        var refreshedCredentials = refreshAccessToken(modCredentials.get());

        if(refreshedCredentials.isEmpty()) {
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
        }  catch (Exception e) {
            log.error("Unexpected error during hasConnected validation: ", e);
            return false;
        }
    }


    private static Optional<ModCredentialsResponse> getModCredentials(String profileId, String serverId, String joinOtp) {
        log.info("Getting mod credentials...");

        try {
            String jsonBody = GsonConfig.toJson(new GetModCredentialsRequest(profileId, serverId, joinOtp));

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
        }  catch (Exception e) {
            log.error("Unexpected error during getting mod credentials: ", e);
            return Optional.empty();
        }
    }

    private static Optional<ModCredentialsResponse> refreshAccessToken(ModCredentials modCredentials) {
        log.info("Refreshing access token...");

        try {
            String jsonBody = GsonConfig.toJson(new RefreshModCredentialsRequest(modCredentials.refreshToken()));

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
        }  catch (Exception e) {
            log.error("Unexpected error during refreshing mod credentials: ", e);
            return Optional.empty();
        }
    }
}
