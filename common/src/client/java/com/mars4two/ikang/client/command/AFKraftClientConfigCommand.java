package com.mars4two.ikang.client.command;

import com.mars4two.ikang.client.core.preferences.ClientPreferencesService;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public final class AFKraftClientConfigCommand {

    private AFKraftClientConfigCommand() {
    }

    public static <S> void register(
            CommandDispatcher<S> dispatcher,
            ClientCommandBridge.SourceAccess<S> sourceAccess
    ) {
        dispatcher.register(
                LiteralArgumentBuilder.<S>literal("afkraft")
                        .then(LiteralArgumentBuilder.<S>literal("config")
                                .executes(context -> {
                                    ClientPreferencesService.requestOpenPreferencesScreen();
                                    return 1;
                                }))
        );
    }
}
