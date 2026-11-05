package com.mars4two.ikang.fabric.client.platform;

import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class FabricClientCommandBridge implements ClientCommandBridge {

    private static final SourceAccess<FabricClientCommandSource> SOURCE_ACCESS = new SourceAccess<>() {
        @Override
        public boolean hasAdminPermission(FabricClientCommandSource source) {
            return ClientUiAdapter.hasAdminPermission(source.permissions());
        }

        @Override
        public void sendFeedback(FabricClientCommandSource source, Component message) {
            source.sendFeedback(message);
        }

        @Override
        public void sendError(FabricClientCommandSource source, Component message) {
            source.sendError(message);
        }
    };

    @Override
    public void registerCommands(CommandRegistrationCallback callback) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            callback.register(dispatcher, SOURCE_ACCESS);
        });
    }
}
