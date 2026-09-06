package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.ClientAuthService;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedRequest;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.IsServerAuthenticatedResponse;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

public class AFKraftClientEnableCommand {

    private AFKraftClientEnableCommand() {
        // Command class, no instances allowed
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("afkraft")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                                .requires(req ->
                                        req.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                                .executes(AFKraftClientEnableCommand::executeClientConnect))
        );

        ClientPlayNetworking.registerGlobalReceiver(IsServerAuthenticatedResponse.TYPE, (payload, context) -> {
            onIsServerAuthenticatedResponse(context.player(), payload.isAuthenticated());
        });
    }

    private static int executeClientConnect(CommandContext<FabricClientCommandSource> context) {
        ClientPlayNetworking.send(new IsServerAuthenticatedRequest());
        return 1;
    }

    private static void onIsServerAuthenticatedResponse(Player player, boolean isAuthenticated) {
        if(isAuthenticated) {
            player.sendSystemMessage(Component.literal("Credenciales de AFKraft activas, generadas por: ..."));
            return;
        }

        player.sendSystemMessage(Component.literal("Generando credenciales AFKraft..."));
        ClientAuthService.authenticate(
                (serverId, joinOtp) -> Minecraft.getInstance().execute(
                        () -> ClientPlayNetworking.send(new GetCredentialsRequestServerboundPayload(player.getGameProfile().id().toString(), serverId, joinOtp))
                ),
                e -> player.sendSystemMessage(Component.literal("Error al generar credenciales AFKraft")),
                () -> player.sendSystemMessage(Component.literal("Aún no has enlazado tu cuenta con AFKraft"))
        );
    }
}
