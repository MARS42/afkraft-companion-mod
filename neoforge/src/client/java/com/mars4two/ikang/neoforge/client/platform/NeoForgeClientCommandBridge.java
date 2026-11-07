package com.mars4two.ikang.neoforge.client.platform;

//? if >=26.2 {
import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
//?}
import com.mars4two.ikang.client.platform.services.ClientCommandBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientCommandBridge implements ClientCommandBridge {

    private static final SourceAccess<CommandSourceStack> SOURCE_ACCESS = new SourceAccess<>() {
        @Override
        public boolean hasAdminPermission(CommandSourceStack source) {
            //? if >=26.2 {
            return ClientUiAdapter.hasAdminPermission(source.permissions());
            //?} else {
            /*return source.hasPermission(2);
            *///?}
        }

        @Override
        public void sendFeedback(CommandSourceStack source, Component message) {
            source.sendSuccess(() -> message, false);
        }

        @Override
        public void sendError(CommandSourceStack source, Component message) {
            source.sendFailure(message);
        }
    };

    @Override
    public void registerCommands(CommandRegistrationCallback callback) {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            callback.register(event.getDispatcher(), SOURCE_ACCESS);
        });
    }
}
