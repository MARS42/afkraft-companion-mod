package com.mars4two.ikang.fabric.client.platform;

import com.mars4two.ikang.client.platform.services.ClientLifecycleHooks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class FabricClientLifecycleHooks implements ClientLifecycleHooks {

    @Override
    public void onClientPlayerJoin(Consumer<Minecraft> listener) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> listener.accept(client));
    }
}
