package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record GetCredentialsStatusClientboundPayload(String profileId, boolean needsCredentials) implements CustomPacketPayload {

    public static final Type<GetCredentialsStatusClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("get_credentials_status_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GetCredentialsStatusClientboundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GetCredentialsStatusClientboundPayload::profileId,
            ByteBufCodecs.BOOL, GetCredentialsStatusClientboundPayload::needsCredentials,
            GetCredentialsStatusClientboundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
