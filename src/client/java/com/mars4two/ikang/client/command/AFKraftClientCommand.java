package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.AuthService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class AFKraftClientCommand {

    private AFKraftClientCommand() {
        // Command class, no instances allowed
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<FabricClientCommandSource>literal("afkraft")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("connect")
                    .executes(AFKraftClientCommand::executeConnect))
        );
    }
    
    private static int executeConnect(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        return AuthService.executeConnect(
                msg -> source.sendFeedback(Component.literal(msg)),
                erroMsg -> source.sendError(Component.literal(erroMsg))
        );
    }
}
