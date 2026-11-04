package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.auth.dtos.AuthorizeServerRequest;
import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.auth.dtos.HasJoinedResponse;
import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;
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

/**
 * Pure domain client authentication workflow decoupling Mojang session handshakes via {@link SessionJoinPort}.
 */
public final class ClientAuthDomainService {

    @FunctionalInterface
    public interface SessionJoinPort {
        void joinServer(UUID profileId, String accessToken, String serverId) throws AuthenticationException;
    }

    public record HasJoinedResult(String serverId, HasJoinedResponse hasJoinedResponse) {
    }

    private static final HttpClient client = HttpConfig.getHttpClient();
    private static final Logger log = LoggerFactory.getLogger("afkraft-client-auth-service");
    private static final AtomicBoolean isConnected = new AtomicBoolean(false);

    private ClientAuthDomainService() {
        // Utility class, no instances allowed
    }

    public static void authenticate(
            UUID profileId,
            String username,
            String accessToken,
            SessionJoinPort sessionJoinPort,
            BiConsumer<String, String> successCallback,
            Consumer<Exception> errorCallback,
            Runnable notConnectedCallback
    ) {
        log.info("Authenticating...");
        CompletableFuture.runAsync(() -> {
            try {
                boolean connected = AuthService.getIsConnected(profileId);
                isConnected.set(connected);
                if (!connected) {
                    log.info("User is not connected");
                    notConnectedCallback.run();
                    return;
                }

                HasJoinedResult joinServerResult = joinServer(profileId, username, accessToken, sessionJoinPort);
                successCallback.accept(joinServerResult.serverId(), joinServerResult.hasJoinedResponse().joinOtp());
            } catch (Exception e) {
                log.error("Error while authentication", e);
                errorCallback.accept(e);
            }
        });
    }

    public static void authorizeServer(
            UUID profileId,
            String username,
            String accessToken,
            SessionJoinPort sessionJoinPort,
            Consumer<HasJoinedResult> successCallback,
            Consumer<Exception> errorCallback,
            Runnable notConnectedCallback
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Authorizing server...");
                boolean connected = AuthService.getIsConnected(profileId);
                if (!connected) {
                    log.info("User is not connected");
                    notConnectedCallback.run();
                    return;
                }

                HasJoinedResult joinServerResult = joinServer(profileId, username, accessToken, sessionJoinPort);
                successCallback.accept(joinServerResult);
            } catch (Exception e) {
                log.error("Error while authentication", e);
                errorCallback.accept(e);
            }
        });
    }

    public static HasJoinedResult joinServer(
            UUID profileId,
            String username,
            String accessToken,
            SessionJoinPort sessionJoinPort
    ) throws AuthenticationException {
        log.debug("[AFKraft] Paso 1: Solicitando serverId al backend...");
        String serverId = AuthService.getServerId(profileId);
        log.info("[AFKraft] serverId obtenido: {}", serverId);

        log.debug("[AFKraft] Paso 2: Realizando joinServer con Mojang...");
        sessionJoinPort.joinServer(profileId, accessToken, serverId);
        log.info("[AFKraft] joinServer completado exitosamente");

        log.debug("[AFKraft] Paso 3: Validando con hasJoined en backend...");
        HasJoinedResponse hasJoinedResponse = AuthService.validateHasJoined(username, serverId, profileId);
        log.info("[AFKraft] Validación hasJoined exitosa");
        return new HasJoinedResult(serverId, hasJoinedResponse);
    }

    public static void sendPlayerAuthorization(
            UUID currentServerId,
            UUID profileId,
            HasJoinedResult joinedResult,
            String serverName,
            Runnable successCallback,
            Runnable errorCallback
    ) {
        CompletableFuture.runAsync(() -> {
            log.info("Sending player authorization request...");
            try {
                String jsonBody = GsonConfig.toJson(new AuthorizeServerRequest(
                        profileId,
                        currentServerId.toString(),
                        joinedResult.serverId(),
                        joinedResult.hasJoinedResponse().joinOtp(),
                        serverName
                ));

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
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.error("Unexpected error during sending player authorization: ", e);
                errorCallback.run();
            }
        });
    }
}
