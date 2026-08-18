package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AFKContainerConfigPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<AFKContainerConfigPayload> TYPE =
            new Type<>(AFKraftCompanion.id("afk_container_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AFKContainerConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, AFKContainerConfigPayload::enabled,
                    AFKContainerConfigPayload::new
            );

    @Override
    public Type<AFKContainerConfigPayload> type() {
        return TYPE;
    }
}
