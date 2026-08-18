package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GetCredentialsRequestClientboundPayload(String profileId, String serverId, String joinOtp) implements CustomPacketPayload {

    public static final Type<GetCredentialsRequestClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("get_credentials_request_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GetCredentialsRequestClientboundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestClientboundPayload::profileId,
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestClientboundPayload::serverId,
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestClientboundPayload::joinOtp,
            GetCredentialsRequestClientboundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
