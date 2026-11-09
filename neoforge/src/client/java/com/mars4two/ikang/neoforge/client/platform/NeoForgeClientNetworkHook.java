package com.mars4two.ikang.neoforge.client.platform;

import com.mars4two.ikang.neoforge.platform.NeoForgeNetworkBridge;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.7 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}

public final class NeoForgeClientNetworkHook implements NeoForgeNetworkBridge.ClientHook {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        //? if >=1.21.7 {
        ClientPacketDistributor.sendToServer(payload);
        //?} else {
        /*PacketDistributor.sendToServer(payload);
        *///?}
    }
}
