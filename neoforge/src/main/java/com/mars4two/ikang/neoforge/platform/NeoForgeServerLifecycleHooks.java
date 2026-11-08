package com.mars4two.ikang.neoforge.platform;

import com.mars4two.ikang.platform.services.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.function.Consumer;

public final class NeoForgeServerLifecycleHooks implements ServerLifecycleHooks {

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> listener) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                listener.accept(serverPlayer);
            }
        });
    }
}
