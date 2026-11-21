package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.util.Codecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateSnapshotPreferencesServerboundPayload(Map<String, Boolean> snapshots) implements CustomPacketPayload {

    public static final Type<UpdateSnapshotPreferencesServerboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("update_snapshot_preferences"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSnapshotPreferencesServerboundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Codecs.SNAPSHOT_PREFERENCES_STREAM_CODEC, UpdateSnapshotPreferencesServerboundPayload::snapshots,
                    UpdateSnapshotPreferencesServerboundPayload::new
            );

    public UpdateSnapshotPreferencesServerboundPayload {
        snapshots = snapshots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
