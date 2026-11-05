package com.mars4two.ikang.fabric.platform;

import com.mars4two.ikang.platform.services.ServerLifecycleHooks;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class FabricServerLifecycleHooks implements ServerLifecycleHooks {

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STARTED.register(listener::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STOPPED.register(listener::accept);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> listener) {
        ServerPlayerEvents.JOIN.register(listener::accept);
    }
}
