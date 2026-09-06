package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IsServerAuthenticatedResponse(boolean isAuthenticated, String credentialsOwner) implements CustomPacketPayload {

    public static final Type<IsServerAuthenticatedResponse> TYPE =
            new Type<>(AFKraftCompanion.id("is_server_authenticated_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsServerAuthenticatedResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, IsServerAuthenticatedResponse::isAuthenticated,
                    ByteBufCodecs.STRING_UTF8, IsServerAuthenticatedResponse::credentialsOwner,
                    IsServerAuthenticatedResponse::new
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
