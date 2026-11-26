package com.mars4two.ikang.core.network.payloads.client;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.util.Codecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SnapshotPreferencesSyncClientboundPayload(
        boolean authenticated,
        boolean editable,
        Map<String, Boolean> snapshots,
        String errorKey
) implements CustomPacketPayload {

    public static final Type<SnapshotPreferencesSyncClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("snapshot_preferences_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPreferencesSyncClientboundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SnapshotPreferencesSyncClientboundPayload::authenticated,
                    ByteBufCodecs.BOOL, SnapshotPreferencesSyncClientboundPayload::editable,
                    Codecs.SNAPSHOT_PREFERENCES_STREAM_CODEC, SnapshotPreferencesSyncClientboundPayload::snapshots,
                    ByteBufCodecs.STRING_UTF8, SnapshotPreferencesSyncClientboundPayload::errorKey,
                    SnapshotPreferencesSyncClientboundPayload::new
            );

    public SnapshotPreferencesSyncClientboundPayload {
        snapshots = snapshots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
        errorKey = errorKey == null ? "" : errorKey;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
