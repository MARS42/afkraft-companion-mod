package com.mars4two.ikang.neoforge.client.platform;

import com.mars4two.ikang.client.platform.services.ClientKeyMappingBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class NeoForgeClientKeyMappingBridge implements ClientKeyMappingBridge {

    private static final List<KeyMapping> REGISTERED_KEYS = new CopyOnWriteArrayList<>();

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : REGISTERED_KEYS) {
            event.register(keyMapping);
        }
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping, Consumer<Minecraft> onPressed) {
        Objects.requireNonNull(keyMapping, "keyMapping");
        Objects.requireNonNull(onPressed, "onPressed");

        REGISTERED_KEYS.add(keyMapping);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft client = Minecraft.getInstance();
            while (keyMapping.consumeClick()) {
                onPressed.accept(client);
            }
        });
    }
}
