package com.mars4two.ikang.network;

import com.mars4two.ikang.core.network.payloads.client.*;
import com.mars4two.ikang.core.network.payloads.server.*;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;


public class PayloadRegister {

    private PayloadRegister() {
        // NO-OP
    }

    public static void registerClientboundPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(AFKContainerConfigData.ContainerData.PAYLOAD_TYPE, AFKContainerConfigData.ContainerData.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GetCredentialsStatusClientboundPayload.TYPE, GetCredentialsStatusClientboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AFKraftStatusClientBoundPayload.TYPE, AFKraftStatusClientBoundPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AuthorizeServerRequestClientboundPayload.TYPE, AuthorizeServerRequestClientboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(IsServerAuthenticatedResponse.TYPE, IsServerAuthenticatedResponse.STREAM_CODEC);
    }

    public static void registerServerboundPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(RegisterAFKContainerServerPayload.PAYLOAD_TYPE, RegisterAFKContainerServerPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GetCredentialsRequestServerboundPayload.TYPE, GetCredentialsRequestServerboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AuthorizeServerResponseServerboundPayload.TYPE, AuthorizeServerResponseServerboundPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(IsServerAuthenticatedRequest.TYPE, IsServerAuthenticatedRequest.STREAM_CODEC);
    }
}
