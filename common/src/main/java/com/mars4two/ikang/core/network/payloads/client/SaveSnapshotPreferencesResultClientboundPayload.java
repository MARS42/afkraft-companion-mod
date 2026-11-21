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

public record SaveSnapshotPreferencesResultClientboundPayload(
        boolean success,
        Map<String, Boolean> snapshots,
        String errorKey
) implements CustomPacketPayload {

    public static final Type<SaveSnapshotPreferencesResultClientboundPayload> TYPE =
            new Type<>(AFKraftCompanion.id("save_snapshot_preferences_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveSnapshotPreferencesResultClientboundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SaveSnapshotPreferencesResultClientboundPayload::success,
                    Codecs.SNAPSHOT_PREFERENCES_STREAM_CODEC, SaveSnapshotPreferencesResultClientboundPayload::snapshots,
                    ByteBufCodecs.STRING_UTF8, SaveSnapshotPreferencesResultClientboundPayload::errorKey,
                    SaveSnapshotPreferencesResultClientboundPayload::new
            );

    public SaveSnapshotPreferencesResultClientboundPayload {
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
