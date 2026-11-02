package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.client.core.auth.dtos.AuthorizeServerRequest;
import com.mars4two.ikang.core.auth.dtos.HasJoinedResponse;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientAuthService {

    private static final HttpClient client = HttpConfig.getHttpClient();
    private static final Logger log = LoggerFactory.getLogger("afkraft-client-auth-service");

    private static final AtomicBoolean isConnected = new AtomicBoolean(false);

    private ClientAuthService() {
        // Utility class, no instances allowed
    }

    public static void authenticate(BiConsumer<String, String> successCallback, Consumer<Exception> errorCallback, Runnable notConnectedCallback) {
        var minecraft = Minecraft.getInstance();
        log.info("Authenticating...");

        CompletableFuture.runAsync(() -> {
            try {
                UUID profileId = minecraft.getUser().getProfileId();
                var isConnected = AuthService.getIsConnected(profileId);
                ClientAuthService.isConnected.set(isConnected);
                if(!isConnected) {
                    log.info("User is not connected");
                    notConnectedCallback.run();
                    return;
                }

                var joinServerResult = joinServer();
                successCallback.accept(joinServerResult.serverId, joinServerResult.hasJoinedResponse.joinOtp());
            } catch (Exception e) {
                log.error("Error while authentication", e);
                errorCallback.accept(e);
            }
        });
    }

    public static void authorizeServer(Consumer<HasJoinedResult> successCallback, Consumer<Exception> errorCallback, Runnable notConnectedCallback) {
        var minecraft = Minecraft.getInstance();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Authorizing server...");

                UUID profileId = minecraft.getUser().getProfileId();
                var isConnected = AuthService.getIsConnected(profileId);
                if(!isConnected) {
                    log.info("User is not connected");
                    notConnectedCallback.run();
                    return;
                }

                var joinServerResult = joinServer();
                successCallback.accept(joinServerResult);
            } catch (Exception e) {
                log.error("Error while authentication", e);
                errorCallback.accept(e);
            }
        });
    }

    public static HasJoinedResult joinServer() throws AuthenticationException, com.mojang.authlib.exceptions.AuthenticationException {
        var minecraft = Minecraft.getInstance();
        var sessionService = minecraft.services().sessionService();

        var user = minecraft.getUser();
        UUID profileId = user.getProfileId();
        String username = user.getName();

        log.debug("[AFKraft] Paso 1: Solicitando serverId al backend...");
        String serverId = AuthService.getServerId(profileId);
//                String serverId = "a7830bd9-3a7d-4792-a9cc-00607520106f";
        log.info("[AFKraft] serverId obtenido: {}", serverId);

        log.debug("[AFKraft] Paso 2: Realizando joinServer con Mojang...");
        sessionService.joinServer(profileId, user.getAccessToken(), serverId);    // Comment in local dev
        log.info("[AFKraft] joinServer completado exitosamente");

        log.debug("[AFKraft] Paso 3: Validando con hasJoined en backend...");
        var hasJoinedResponse = AuthService.validateHasJoined(username, serverId, profileId);       // Comment in local dev
        log.info("[AFKraft] Validación hasJoined exitosa");
        return new HasJoinedResult(serverId, hasJoinedResponse);
    }

    public record HasJoinedResult(String serverId, HasJoinedResponse hasJoinedResponse) { }

    public static void sendPlayerAuthorization(UUID currentServerId, UUID profileId, HasJoinedResult joinedResult, String serverName, Runnable successCallback, Runnable errorCallback) {
        CompletableFuture.runAsync(() -> {
            log.info("Sending player authorization request...");

            try {
                String jsonBody = GsonConfig.toJson(new AuthorizeServerRequest(profileId,
                        currentServerId.toString(),
                        joinedResult.serverId(),
                        joinedResult.hasJoinedResponse().joinOtp(),
                        serverName));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(HttpConfig.getSendPlayerAuthorizationUri())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("Error sending player authorization request (HTTP {})", response.statusCode());
                    errorCallback.run();
                    return;
                }
                log.info("Player authorization request sent successfully");
                successCallback.run();
            }  catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error("Unexpected error during refreshing mod credentials: ", e);
                errorCallback.run();
            }
        });
    }
}
