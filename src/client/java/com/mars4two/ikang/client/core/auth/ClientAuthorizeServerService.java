package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.screens.AuthorizeServerRequestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ClientAuthorizeServerService {

    private final Player player;
    private final Consumer<ClientAuthService.HasJoinedResult> hasJoinedResultConsumer;
    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-server-service");

    public ClientAuthorizeServerService(Player player, Consumer<ClientAuthService.HasJoinedResult> hasJoinedResultConsumer) {
        this.player = player;
        this.hasJoinedResultConsumer = hasJoinedResultConsumer;
    }

    public void showAuthorizationScreen() {
        Minecraft client = Minecraft.getInstance();
        var serverData = client.getCurrentServer();
        if(serverData == null) {
            log.info("No server selected");
            return;
        }

        var serverName = serverData.name;
        client.execute(() -> client.setScreenAndShow(new AuthorizeServerRequestScreen(
                null,
                serverName,
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



    private void successAuthorizeServer(ClientAuthService.HasJoinedResult hasJoinedResult) {
        log.info("Server authorized successfully");
        hasJoinedResultConsumer.accept(hasJoinedResult);
        player.sendSystemMessage(Component.literal("Servidor autorizado correctamente"));
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
