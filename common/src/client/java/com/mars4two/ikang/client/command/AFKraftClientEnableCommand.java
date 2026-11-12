package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.ClientAuthService;
import com.mars4two.ikang.client.core.events.ClientEvents;
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedResponse;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.IsServerAuthenticatedRequest;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mars4two.ikang.platform.services.Services;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class AFKraftClientEnableCommand {

    private AFKraftClientEnableCommand() {
    }

    public static <S> void register(
            CommandDispatcher<S> dispatcher,
            ClientCommandBridge.SourceAccess<S> sourceAccess
    ) {
        dispatcher.register(
                LiteralArgumentBuilder.<S>literal("afkraft")
                        .then(LiteralArgumentBuilder.<S>literal("enable")
                                .requires(sourceAccess::hasAdminPermission)
                                .executes(context -> {
                                    Services.NETWORK.sendToServer(new IsServerAuthenticatedRequest());
                                    return 1;
                                }))
        );
    }

    public static void onIsServerAuthenticatedResponse(Player player, IsServerAuthenticatedResponse payload) {
        if (payload.isAuthenticated()) {
            ClientUiAdapter.sendPlayerMessage(
                    player,
                    AFKraftTranslations.translatable(AFKraftTranslations.CMD_ENABLE_ALREADY_ACTIVE, payload.credentialsOwner())
            );
            return;
        }

        ClientUiAdapter.sendPlayerMessage(player, AFKraftTranslations.translatable(AFKraftTranslations.CMD_ENABLE_GENERATING));
        ClientAuthService.authenticate(
                (serverId, joinOtp) -> Minecraft.getInstance().execute(
                        () -> Services.NETWORK.sendToServer(new GetCredentialsRequestServerboundPayload(
                                WorldAndEntityAdapter.playerId(player).toString(),
                                serverId,
                                joinOtp,
                                ClientEvents.getWorldName()
                        ))
                ),
                e -> ClientUiAdapter.sendPlayerMessage(player, AFKraftTranslations.translatable(AFKraftTranslations.CMD_ENABLE_ERROR)),
                () -> ClientUiAdapter.sendPlayerMessage(player, AFKraftTranslations.translatable(AFKraftTranslations.CMD_ENABLE_NOT_LINKED))
        );
    }
}
