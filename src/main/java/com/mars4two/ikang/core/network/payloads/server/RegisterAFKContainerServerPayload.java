package com.mars4two.ikang.core.network.payloads.server;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.storage.ContainerTrackingMode;
import com.mars4two.ikang.core.util.Codecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record RegisterAFKContainerServerPayload(String containerType, boolean enabled, ContainerTrackingMode mode) implements CustomPacketPayload {

    public static final Identifier REGISTER_AFK_CONTAINER_ID = AFKraftCompanion.id("register_afk_container_payload");

    public static final CustomPacketPayload.Type<RegisterAFKContainerServerPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(REGISTER_AFK_CONTAINER_ID);

    public RegisterAFKContainerServerPayload(String containerType, ContainerTrackingMode mode) {
        this(containerType, mode != null && mode.isEnabled(), mode != null ? mode : ContainerTrackingMode.OFF);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RegisterAFKContainerServerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    RegisterAFKContainerServerPayload::containerType,
                    ByteBufCodecs.BOOL,
                    RegisterAFKContainerServerPayload::enabled,
                    Codecs.CONTAINER_TRACKING_MODE_STREAM_CODEC,
                    RegisterAFKContainerServerPayload::mode,
                    RegisterAFKContainerServerPayload::new
            );

    @Override
    public @NonNull Type<RegisterAFKContainerServerPayload> type() {
        return PAYLOAD_TYPE;
    }
}
