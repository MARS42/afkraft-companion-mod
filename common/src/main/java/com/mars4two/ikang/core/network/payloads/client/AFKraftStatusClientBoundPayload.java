package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AFKraftStatusClientBoundPayload(
        boolean enabled,
        boolean authenticated,
        AFKContainerConfigData.ContainerData containerData
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AFKraftStatusClientBoundPayload> TYPE =
            new CustomPacketPayload.Type<>(AFKraftCompanion.id("afkraft_status_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AFKraftStatusClientBoundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AFKraftStatusClientBoundPayload::enabled,
            ByteBufCodecs.BOOL, AFKraftStatusClientBoundPayload::authenticated,
            AFKContainerConfigData.ContainerData.STREAM_CODEC, AFKraftStatusClientBoundPayload::containerData,
            AFKraftStatusClientBoundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
