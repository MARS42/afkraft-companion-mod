package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.auth.ClientConnectService;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.Component;

public final class AFKraftClientConnectCommand {

    private AFKraftClientConnectCommand() {
    }

    public static <S> void register(
            CommandDispatcher<S> dispatcher,
            ClientCommandBridge.SourceAccess<S> sourceAccess
    ) {
        dispatcher.register(
                LiteralArgumentBuilder.<S>literal("afkraft")
                        .then(LiteralArgumentBuilder.<S>literal("connect")
                                .executes(context -> {
                                    S source = context.getSource();
                                    return ClientConnectService.executeConnect(
                                            msg -> sourceAccess.sendFeedback(source, Component.literal(msg)),
                                            errorMsg -> sourceAccess.sendError(source, Component.literal(errorMsg))
                                    );
                                }))
        );
    }
}
