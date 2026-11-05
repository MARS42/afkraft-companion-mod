package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuthorizeServerRequestClientboundPayload(String currentServerId) implements CustomPacketPayload {

    public static final Type<AuthorizeServerRequestClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("authorize_server_request_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizeServerRequestClientboundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AuthorizeServerRequestClientboundPayload::currentServerId,
            AuthorizeServerRequestClientboundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
