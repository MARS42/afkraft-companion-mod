package com.mars4two.ikang.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public interface ServerLifecycleHooks {

    void onServerStarted(Consumer<MinecraftServer> listener);

    void onServerStopped(Consumer<MinecraftServer> listener);

    void onPlayerJoin(Consumer<ServerPlayer> listener);
}
