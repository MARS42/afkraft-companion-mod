package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.auth.dtos.HasJoinedResponse;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientAuthService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-service");

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
}
