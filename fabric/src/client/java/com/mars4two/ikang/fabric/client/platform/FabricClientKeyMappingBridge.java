package com.mars4two.ikang.fabric.client.platform;

import com.mars4two.ikang.client.platform.services.ClientKeyMappingBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.Consumer;

public final class FabricClientKeyMappingBridge implements ClientKeyMappingBridge {

    @Override
    public void registerKeyMapping(KeyMapping keyMapping, Consumer<Minecraft> onPressed) {
        Objects.requireNonNull(keyMapping, "keyMapping");
        Objects.requireNonNull(onPressed, "onPressed");

        //? if >=26.1 {
        KeyMappingHelper.registerKeyMapping(keyMapping);
        //?} else {
        /*KeyBindingHelper.registerKeyBinding(keyMapping);
        *///?}

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyMapping.consumeClick()) {
                onPressed.accept(client);
            }
        });
    }
}
