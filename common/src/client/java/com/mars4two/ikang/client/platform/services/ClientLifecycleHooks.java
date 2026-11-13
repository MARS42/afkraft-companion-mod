package com.mars4two.ikang.client.platform.services;

import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public interface ClientLifecycleHooks {

    void onClientPlayerJoin(Consumer<Minecraft> listener);
}
