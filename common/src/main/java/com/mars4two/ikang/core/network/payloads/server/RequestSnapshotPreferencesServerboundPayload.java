package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestSnapshotPreferencesServerboundPayload() implements CustomPacketPayload {

    public static final Type<RequestSnapshotPreferencesServerboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("request_snapshot_preferences"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSnapshotPreferencesServerboundPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestSnapshotPreferencesServerboundPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
