package com.mars4two.ikang.client.platform.services;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/**
 * Loader-neutral SPI for registering client key mappings and reacting to key presses on client ticks.
 */
public interface ClientKeyMappingBridge {

    void registerKeyMapping(KeyMapping keyMapping, Consumer<Minecraft> onPressed);
}
