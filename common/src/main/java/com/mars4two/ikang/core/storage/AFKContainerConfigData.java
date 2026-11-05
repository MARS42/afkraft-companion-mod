package com.mars4two.ikang.core.storage;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.model.DimensionKey;
import com.mars4two.ikang.core.util.Codecs;
import com.mars4two.ikang.core.util.PortableContainerIdentityHelper;
import com.mars4two.ikang.platform.mc.SavedDataAdapter;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AFKContainerConfigData extends SavedData {

    public static final Logger log = LoggerFactory.getLogger("AFKContainerConfigData");

    private final ContainerTrackingService trackingService;

    public record ContainerData(boolean enabled, ContainerTrackingMode mode) implements CustomPacketPayload {

        public static final Identifier ID = AFKraftCompanion.id("afk_container_data_payload");

        public static final Type<ContainerData> PAYLOAD_TYPE = new Type<>(ID);

        public ContainerData(boolean enabled) {
            this(enabled, enabled ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
        }

        public ContainerData(ContainerTrackingMode mode) {
            this(mode != null && mode.isEnabled(), mode != null ? mode : ContainerTrackingMode.OFF);
        }

        public static ContainerData fromDomain(ContainerTrackingEntry entry) {
            return new ContainerData(entry.enabled(), entry.mode());
        }

        public ContainerTrackingEntry toDomain() {
            return new ContainerTrackingEntry(enabled, mode);
        }

        public static final Codec<ContainerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.fieldOf("enabled").forGetter(ContainerData::enabled),
                        Codecs.CONTAINER_TRACKING_MODE_CODEC.optionalFieldOf("mode").forGetter(data -> Optional.of(data.mode()))
                )
                .apply(instance, (enabled, modeOpt) -> {
                    ContainerTrackingMode resolvedMode = modeOpt.orElse(enabled ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
                    if (!enabled) {
                        resolvedMode = ContainerTrackingMode.OFF;
                    }
                    return new ContainerData(enabled, resolvedMode);
                }));

        public static final StreamCodec<RegistryFriendlyByteBuf, ContainerData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                ContainerData::enabled,
                Codecs.CONTAINER_TRACKING_MODE_STREAM_CODEC,
                ContainerData::mode,
                ContainerData::new
        );

        @Override
        public Type<ContainerData> type() {
            return PAYLOAD_TYPE;
        }
    }

    public AFKContainerConfigData() {
        this.trackingService = new ContainerTrackingService();
    }

    public AFKContainerConfigData(
            Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> afkContainerConfigData,
            Map<UUID, List<UUID>> portableContainersData
    ) {
        var domainState = new HashMap<UUID, Map<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>>();
        if (afkContainerConfigData != null) {
            afkContainerConfigData.forEach((playerId, byDim) -> {
                var dimMap = new HashMap<DimensionKey, Map<BlockCoord, ContainerTrackingEntry>>();
                byDim.forEach((dimId, byPos) -> {
                    var posMap = new HashMap<BlockCoord, ContainerTrackingEntry>();
                    byPos.forEach((pos, data) -> posMap.put(toCoord(pos), data.toDomain()));
                    dimMap.put(toDimKey(dimId), posMap);
                });
                domainState.put(playerId, dimMap);
            });
        }
        this.trackingService = new ContainerTrackingService(domainState, portableContainersData);
    }

    public static final Codec<AFKContainerConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codecs.UUID_CODEC,
                            Codec.unboundedMap(Identifier.CODEC,
                                    Codec.unboundedMap(Codecs.STRING_BLOCK_POS_CODEC, ContainerData.CODEC))
                    ).fieldOf("config").forGetter(AFKContainerConfigData::getConfig),
                    Codec.unboundedMap(Codecs.UUID_CODEC,
                            Codecs.UUID_CODEC.listOf()
                    ).optionalFieldOf("portable_containers", Map.of()).forGetter(AFKContainerConfigData::exportPortableContainers)
            )
            .apply(instance, AFKContainerConfigData::new));

    public static AFKContainerConfigData getConfig(MinecraftServer server) {
        return SavedDataAdapter.getOrCreateConfig(server);
    }

    public void set(ServerPlayer player, BlockPos pos, boolean active) {
        setPositionalMode(player, pos, active ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
    }

    public void setPositionalMode(ServerPlayer player, BlockPos pos, ContainerTrackingMode mode) {
        trackingService.setPositionalMode(
                WorldAndEntityAdapter.playerId(player),
                WorldAndEntityAdapter.dimensionKey(player.level()),
                toCoord(pos),
                mode
        );
        setDirty();
    }

    public void setPortableContainer(ServerPlayer player, UUID containerId, boolean active) {
        trackingService.setPortableContainer(WorldAndEntityAdapter.playerId(player), containerId, active);
        setDirty();
    }

    public boolean isPortableContainerEnabled(UUID containerId) {
        return trackingService.isPortableContainerEnabled(containerId);
    }

    public boolean isPortableContainerEnabledFor(UUID playerId, UUID containerId) {
        return trackingService.isPortableContainerEnabledFor(playerId, containerId);
    }

    public Set<UUID> getPortableContainerOwners(UUID containerId) {
        return trackingService.getPortableContainerOwners(containerId);
    }

    public ContainerData resolveEffectiveData(ServerPlayer player, BlockPos pos, BlockEntity blockEntity) {
        Optional<UUID> containerId = blockEntity != null
                ? PortableContainerIdentityHelper.getContainerId(blockEntity)
                : Optional.empty();
        ContainerTrackingEntry entry = trackingService.resolveEffectiveEntry(
                WorldAndEntityAdapter.playerId(player),
                WorldAndEntityAdapter.dimensionKey(player.level()),
                toCoord(pos),
                containerId
        );
        return ContainerData.fromDomain(entry);
    }

    public Optional<ContainerData> getContainerData(ServerPlayer serverPlayer, BlockPos pos) {
        return trackingService.getContainerEntry(
                WorldAndEntityAdapter.playerId(serverPlayer),
                WorldAndEntityAdapter.dimensionKey(serverPlayer.level()),
                toCoord(pos)
        ).map(ContainerData::fromDomain);
    }

    public void deleteContainerData(ServerPlayer player, BlockPos pos) {
        var playerId = WorldAndEntityAdapter.playerId(player);
        var dimKey = WorldAndEntityAdapter.dimensionKey(player.level());
        if (trackingService.deleteContainerData(playerId, dimKey, toCoord(pos))) {
            log.info("Container data has been deleted from the server for player {}", playerId);
            setDirty();
        }
    }

    public void deleteFixedContainersAt(Identifier dimensionId, BlockPos pos) {
        if (trackingService.deleteFixedContainersAt(toDimKey(dimensionId), toCoord(pos))) {
            log.info("Fixed container data at {} in {} has been deleted for all owners", pos, dimensionId);
            setDirty();
        }
    }

    public boolean isContainerEnabledIn(ServerPlayer player, List<BlockPos> positions) {
        List<BlockCoord> coords = positions.stream().map(AFKContainerConfigData::toCoord).toList();
        return trackingService.isContainerEnabledIn(
                WorldAndEntityAdapter.playerId(player),
                WorldAndEntityAdapter.dimensionKey(player.level()),
                coords
        );
    }

    public boolean isContainerEnabledAt(Identifier dimensionId, BlockPos pos) {
        return trackingService.isContainerEnabledAt(toDimKey(dimensionId), toCoord(pos));
    }

    public Set<UUID> getEnabledOwnersAt(Identifier dimensionId, BlockPos pos) {
        return trackingService.getEnabledOwnersAt(toDimKey(dimensionId), toCoord(pos));
    }

    public boolean isLoadStationEnabledAt(Identifier dimensionId, BlockPos pos) {
        return trackingService.isLoadStationEnabledAt(toDimKey(dimensionId), toCoord(pos));
    }

    public Set<UUID> getLoadStationOwnersAt(Identifier dimensionId, BlockPos pos) {
        return trackingService.getLoadStationOwnersAt(toDimKey(dimensionId), toCoord(pos));
    }

    public Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> getConfig() {
        var result = new HashMap<UUID, Map<Identifier, Map<BlockPos, ContainerData>>>();
        trackingService.exportPositionalState().forEach((playerId, byDim) -> {
            var dimMap = new HashMap<Identifier, Map<BlockPos, ContainerData>>();
            byDim.forEach((dimKey, byPos) -> {
                var posMap = new HashMap<BlockPos, ContainerData>();
                byPos.forEach((coord, entry) -> posMap.put(toBlockPos(coord), ContainerData.fromDomain(entry)));
                dimMap.put(WorldAndEntityAdapter.parseId(dimKey.value()), posMap);
            });
            result.put(playerId, dimMap);
        });
        return result;
    }

    public Map<UUID, List<UUID>> exportPortableContainers() {
        return trackingService.exportPortableContainers();
    }

    private static DimensionKey toDimKey(Identifier id) {
        return new DimensionKey(id.toString());
    }

    private static BlockCoord toCoord(BlockPos pos) {
        return new BlockCoord(pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos toBlockPos(BlockCoord coord) {
        return new BlockPos(coord.x(), coord.y(), coord.z());
    }
}
