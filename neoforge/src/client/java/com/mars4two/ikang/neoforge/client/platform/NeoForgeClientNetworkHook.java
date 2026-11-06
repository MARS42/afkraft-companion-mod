package com.mars4two.ikang.neoforge.client.platform;

import com.mars4two.ikang.neoforge.platform.NeoForgeNetworkBridge;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class NeoForgeClientNetworkHook implements NeoForgeNetworkBridge.ClientHook {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
