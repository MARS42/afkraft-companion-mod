package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GetCredentialsRequestServerboundPayload(String profileId, String serverId, String joinOtp) implements CustomPacketPayload {

    public static final Type<GetCredentialsRequestServerboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("get_credentials_request_server_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GetCredentialsRequestServerboundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestServerboundPayload::profileId,
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestServerboundPayload::serverId,
            ByteBufCodecs.STRING_UTF8, GetCredentialsRequestServerboundPayload::joinOtp,
            GetCredentialsRequestServerboundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
