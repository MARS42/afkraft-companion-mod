package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.ClientAuthService;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

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
    }

    private static int executeClientConnect(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        var profileId = source.getPlayer().getGameProfile().id();
        source.sendFeedback(Component.literal("Generando credenciales AFKraft..."));

        ClientAuthService.authenticate(
                (serverId, joinOtp) -> Minecraft.getInstance().execute(
                        () -> ClientPlayNetworking.send(new GetCredentialsRequestServerboundPayload(profileId.toString(), serverId, joinOtp))
                ),
                e -> source.sendError(Component.literal("Error al generar credenciales AFKraft")),
                () -> source.sendFeedback(Component.literal("Aún no has enlazado tu cuenta con AFKraft"))
        );
        return 1;
    }
}
