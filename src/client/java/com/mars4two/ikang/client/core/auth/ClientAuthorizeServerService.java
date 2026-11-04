package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.core.screens.AuthorizeServerRequestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClientAuthorizeServerService {

    private final UUID currentServerId;
    private final Player player;
    private final Runnable hasAuthorizedCallback;

    private final Minecraft minecraft;

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-server-service");

    public ClientAuthorizeServerService(UUID currentServerId, Player player, Runnable hasAuthorizedCallback) {
        this.currentServerId = currentServerId;
        this.player = player;
        this.hasAuthorizedCallback = hasAuthorizedCallback;

        this.minecraft = Minecraft.getInstance();
    }

    public void showAuthorizationScreen() {
        minecraft.execute(() -> minecraft.setScreenAndShow(new AuthorizeServerRequestScreen(
                null,
                ClientEvents.getWorldName(),
                authorized -> {
                    if (!Boolean.TRUE.equals(authorized)) {
                        log.info("User doesn't want authorize the server");
                        return;
                    }
                    ClientAuthService.authorizeServer(this::successAuthorizeServer,
                            this::failAuthorizeServer,
                            this::notConnectedCallback);
                }
        )));
    }

    private void successAuthorizeServer(ClientAuthDomainService.HasJoinedResult hasJoinedResult) {
        ClientAuthService.sendPlayerAuthorization(currentServerId, player.getGameProfile().id(), hasJoinedResult,
                ClientEvents.getWorldName(),
                () -> {
                    log.info("Server authorized by player successfully");
                    minecraft.execute(() -> {
                        hasAuthorizedCallback.run();
                        player.sendSystemMessage(Component.literal("Servidor autorizado correctamente"));
                    });
                },
                () -> minecraft.execute(() -> player.sendSystemMessage(Component.literal("Error al autorizar el servidor"))));
    }

    private void failAuthorizeServer(Exception e) {
        log.error("Error while authorizing server", e);
        player.sendSystemMessage(Component.literal("Ocurrió un error al autorizar el servidor, intenta más tarde"));
    }

    private void notConnectedCallback() {
        log.info("User is not connected");
        player.sendSystemMessage(Component.literal("Debes estar conectado para autorizar un servidor"));
    }
}
