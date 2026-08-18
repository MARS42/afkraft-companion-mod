package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.ClientConnectService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class AFKraftClientConnectCommand {

    private AFKraftClientConnectCommand() {
        // Command class, no instances allowed
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("afkraft")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("connect")
                                .executes(AFKraftClientConnectCommand::executeClientConnect))
        );
    }

    private static int executeClientConnect(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        return ClientConnectService.executeConnect(
                msg -> source.sendFeedback(Component.literal(msg)),
                erroMsg -> source.sendError(Component.literal(erroMsg))
        );
    }
}
