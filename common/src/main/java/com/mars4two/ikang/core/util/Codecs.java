package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.storage.ContainerTrackingMode;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Codecs {
    public static final int MAX_SNAPSHOT_PREFERENCES_ENTRIES = 100;
    public static final int MAX_SNAPSHOT_NAME_LENGTH = 64;

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<BlockPos> STRING_BLOCK_POS_CODEC = Codec.STRING.xmap(s -> {
        String[] split = s.split(",");
        return new BlockPos(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }, blockPos -> String.format("%d,%d,%d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));

    public static final Codec<ContainerTrackingMode> CONTAINER_TRACKING_MODE_CODEC = Codec.STRING.xmap(
            ContainerTrackingMode::fromSerializedName,
            ContainerTrackingMode::getSerializedName
    );

    public static final StreamCodec<ByteBuf, ContainerTrackingMode> CONTAINER_TRACKING_MODE_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    ContainerTrackingMode::fromSerializedName,
                    ContainerTrackingMode::getSerializedName
            );

    public static final StreamCodec<ByteBuf, Map<String, Boolean>> SNAPSHOT_PREFERENCES_STREAM_CODEC =
            ByteBufCodecs.map(
                    LinkedHashMap::new,
                    ByteBufCodecs.stringUtf8(MAX_SNAPSHOT_NAME_LENGTH),
                    ByteBufCodecs.BOOL,
                    MAX_SNAPSHOT_PREFERENCES_ENTRIES
            );

    private Codecs() {
    }
}
