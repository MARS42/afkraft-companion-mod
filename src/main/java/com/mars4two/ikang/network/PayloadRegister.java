package com.mars4two.ikang.network;

import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsStatusServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;


public class PayloadRegister {

    private PayloadRegister() {
        // NO-OP
    }

    public static void registerClientboundPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(AFKContainerConfigData.ContainerData.PAYLOAD_TYPE, AFKContainerConfigData.ContainerData.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GetCredentialsStatusClientboundPayload.TYPE, GetCredentialsStatusClientboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GetCredentialsRequestClientboundPayload.TYPE, GetCredentialsRequestClientboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AFKraftStatusClientBoundPayload.TYPE, AFKraftStatusClientBoundPayload.STREAM_CODEC);
    }

    public static void registerServerboundPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(RegisterAFKContainerServerPayload.PAYLOAD_TYPE, RegisterAFKContainerServerPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GetCredentialsStatusServerboundPayload.TYPE, GetCredentialsStatusServerboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GetCredentialsRequestServerboundPayload.TYPE, GetCredentialsRequestServerboundPayload.STREAM_CODEC);
    }
}
