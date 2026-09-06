package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AuthorizeServerResponseServerboundPayload(String serverName, String serverId, String joinOtp) implements CustomPacketPayload {

    public static final Type<AuthorizeServerResponseServerboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("authorize_server_response_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizeServerResponseServerboundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AuthorizeServerResponseServerboundPayload::serverName,
                    ByteBufCodecs.STRING_UTF8, AuthorizeServerResponseServerboundPayload::serverId,
                    ByteBufCodecs.STRING_UTF8, AuthorizeServerResponseServerboundPayload::joinOtp,
                    AuthorizeServerResponseServerboundPayload::new
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
