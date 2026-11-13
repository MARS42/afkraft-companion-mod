package com.mars4two.ikang.neoforge.client.platform;

import com.mars4two.ikang.client.platform.services.ClientLifecycleHooks;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

public final class NeoForgeClientLifecycleHooks implements ClientLifecycleHooks {

    @Override
    public void onClientPlayerJoin(Consumer<Minecraft> listener) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            listener.accept(Minecraft.getInstance());
        });
    }
}
