package com.mars4two.ikang.client.platform.services;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.network.chat.Component;

public interface ClientCommandBridge {

    interface SourceAccess<S> {
        boolean hasAdminPermission(S source);

        void sendFeedback(S source, Component message);

        void sendError(S source, Component message);
    }

    @FunctionalInterface
    interface CommandRegistrationCallback {
        <S> void register(CommandDispatcher<S> dispatcher, SourceAccess<S> sourceAccess);
    }

    void registerCommands(CommandRegistrationCallback callback);
}
