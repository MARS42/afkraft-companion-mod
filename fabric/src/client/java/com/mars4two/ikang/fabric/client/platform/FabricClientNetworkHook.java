package com.mars4two.ikang.fabric.client.platform;

import com.mars4two.ikang.fabric.platform.FabricNetworkBridge;
import com.mars4two.ikang.platform.services.NetworkBridge;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientNetworkHook implements FabricNetworkBridge.ClientHook {

    @Override
    public <T extends CustomPacketPayload> void registerReceiver(
            CustomPacketPayload.Type<T> type,
            NetworkBridge.ClientPayloadHandler<T> handler
    ) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var client = context.client();
            if (client.isSameThread()) {
                handler.handle(payload, context.player());
            } else {
                client.execute(() -> handler.handle(payload, client.player));
            }
        });
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
