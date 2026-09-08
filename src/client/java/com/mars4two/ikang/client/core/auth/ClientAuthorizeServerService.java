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
    private final String serverName;
    private final Consumer<AuthorizeServerResult> hasJoinedResultConsumer;
    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-server-service");

    public ClientAuthorizeServerService(Player player, Consumer<AuthorizeServerResult> hasJoinedResultConsumer) {
        this.player = player;
        this.hasJoinedResultConsumer = hasJoinedResultConsumer;

        var serverData = Minecraft.getInstance().getCurrentServer();
        if(serverData == null) {
            throw new IllegalStateException("No server selected");
        }

        this.serverName = serverData.name;
    }

    public void showAuthorizationScreen() {
        var client = Minecraft.getInstance();
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
        hasJoinedResultConsumer.accept(new AuthorizeServerResult(serverName, hasJoinedResult));
    }

    private void failAuthorizeServer(Exception e) {
        log.error("Error while authorizing server", e);
        player.sendSystemMessage(Component.literal("Ocurrió un error al autorizar el servidor, intenta más tarde"));
    }

    private void notConnectedCallback() {
        log.info("User is not connected");
        player.sendSystemMessage(Component.literal("Debes estar conectado para autorizar un servidor"));
    }

    public record AuthorizeServerResult(String serverName, ClientAuthService.HasJoinedResult hasJoinedResult) {
    }
}
