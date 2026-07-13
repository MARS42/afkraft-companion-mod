package com.mars4two.ikang.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class AFKraftClientCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<FabricClientCommandSource>literal("afkraft")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("connect")
                    .executes(AFKraftClientCommand::executeConnect))
        );
    }
    
    private static int executeConnect(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player == null) {
            source.sendError(Component.literal("Debes estar en un mundo para usar este comando"));
            return 0;
        }
        
        source.sendFeedback(Component.literal("Iniciando enlace..."));
        
        var sessionService = minecraft.services().sessionService();
        var user = minecraft.getUser();
        String serverId = "afkraft-" + System.currentTimeMillis();
        
        // joinServer es una llamada de red, ejecutar en hilo separado
        CompletableFuture.runAsync(() -> {
            try {
                sessionService.joinServer(user.getProfileId(), user.getAccessToken(), serverId);
                minecraft.execute(() -> 
                    source.sendFeedback(Component.literal("Conexión establecida correctamente"))
                );
            } catch (Exception e) {
                minecraft.execute(() -> 
                    source.sendError(Component.literal("Error al conectar: " + e.getMessage()))
                );
            }
        });
        
        return 1;
    }
}
