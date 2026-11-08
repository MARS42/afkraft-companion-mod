package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IsServerAuthenticatedRequest() implements CustomPacketPayload {

    public static final Type<IsServerAuthenticatedRequest> TYPE =
            new Type<>(AFKraftCompanion.id("is_server_authenticated_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsServerAuthenticatedRequest> STREAM_CODEC =
            StreamCodec.unit(new IsServerAuthenticatedRequest());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
