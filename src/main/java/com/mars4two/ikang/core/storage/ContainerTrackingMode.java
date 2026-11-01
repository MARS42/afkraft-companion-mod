package com.mars4two.ikang.core.storage;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum ContainerTrackingMode implements StringRepresentable {
    OFF("off"),
    CONTAINER("container"),
    PORTABLE_CONTAINER("portable_container"),
    LOAD_STATION("load_station");

    public static final Codec<ContainerTrackingMode> CODEC = StringRepresentable.fromEnum(ContainerTrackingMode::values);

    public static final StreamCodec<ByteBuf, ContainerTrackingMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            ContainerTrackingMode::fromSerializedName,
            ContainerTrackingMode::getSerializedName
    );

    private final String serializedName;

    ContainerTrackingMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }

    public boolean isEnabled() {
        return this != OFF;
    }

    public static ContainerTrackingMode fromSerializedName(String name) {
        for (ContainerTrackingMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return OFF;
    }
}
