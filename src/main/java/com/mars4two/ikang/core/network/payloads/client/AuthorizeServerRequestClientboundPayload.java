package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record AuthorizeServerRequestClientboundPayload() implements CustomPacketPayload {

    public static final Type<AuthorizeServerRequestClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("authorize_server_request_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizeServerRequestClientboundPayload> STREAM_CODEC = StreamCodec.unit(new AuthorizeServerRequestClientboundPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
