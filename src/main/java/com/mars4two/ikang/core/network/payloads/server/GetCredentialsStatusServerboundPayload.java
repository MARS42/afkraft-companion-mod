package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GetCredentialsStatusServerboundPayload(boolean connected) implements CustomPacketPayload {

    public static final Type<GetCredentialsStatusServerboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("get_credentials_status_response_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GetCredentialsStatusServerboundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, GetCredentialsStatusServerboundPayload::connected,
                    GetCredentialsStatusServerboundPayload::new
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
