package com.mars4two.ikang.core.storage;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.util.Codecs;
import com.mars4two.ikang.core.util.PortableContainerIdentityHelper;
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
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AFKContainerConfigData extends SavedData {

    public static final Logger log = LoggerFactory.getLogger("AFKContainerConfigData");

    private static final Identifier ID = AFKraftCompanion.id("afk_container_config_data");
    private static final Identifier LEGACY_ID = Identifier.fromNamespaceAndPath(AFKraftCompanion.LEGACY_MOD_ID, "afk_container_config_data");

    private final HashMap<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> config;
    private final HashMap<UUID, Set<UUID>> portableContainers;

    // Índices inversos derivados (NO se serializan)
    private final transient Map<Identifier, Map<BlockPos, Set<UUID>>> enabledIndex = new HashMap<>();
    private final transient Map<Identifier, Map<BlockPos, Set<UUID>>> loadStationIndex = new HashMap<>();
    private final transient Map<UUID, Set<UUID>> portableIndex = new HashMap<>();

    public record ContainerData(boolean enabled, ContainerTrackingMode mode) implements CustomPacketPayload {

        public static final Identifier ID = AFKraftCompanion.id("afk_container_data_payload");

        public static final Type<ContainerData> PAYLOAD_TYPE = new Type<>(ID);

        public ContainerData(boolean enabled) {
            this(enabled, enabled ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
        }

        public ContainerData(ContainerTrackingMode mode) {
            this(mode != null && mode.isEnabled(), mode != null ? mode : ContainerTrackingMode.OFF);
        }

        public static final Codec<ContainerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.fieldOf("enabled").forGetter(ContainerData::enabled),
                        ContainerTrackingMode.CODEC.optionalFieldOf("mode").forGetter(data -> Optional.of(data.mode()))
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
                ContainerTrackingMode.STREAM_CODEC,
                ContainerData::mode,
                ContainerData::new
        );

        @Override
        public Type<ContainerData> type() {
            return PAYLOAD_TYPE;
        }
    }

    public AFKContainerConfigData() {
        this.config = new HashMap<>();
        this.portableContainers = new HashMap<>();
    }

    public AFKContainerConfigData(
            Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> afkContainerConfigData,
            Map<UUID, List<UUID>> portableContainersData
    ) {
        var deepMutableCopy = new HashMap<UUID, Map<Identifier, Map<BlockPos, ContainerData>>>();
        afkContainerConfigData.forEach((k, v) -> {
            deepMutableCopy.put(k, new HashMap<>(v));
            v.forEach((k1, v1) -> {
                deepMutableCopy.get(k).put(k1, new HashMap<>(v1));
                v1.forEach((k2, v2) -> deepMutableCopy.get(k).get(k1).put(k2, v2));
            });
        });

        var portableMutableCopy = new HashMap<UUID, Set<UUID>>();
        if (portableContainersData != null) {
            portableContainersData.forEach((playerId, ids) -> portableMutableCopy.put(playerId, new HashSet<>(ids)));
        }

        this.config = deepMutableCopy;
        this.portableContainers = portableMutableCopy;
        rebuildIndex();
    }

    private void rebuildIndex() {
        enabledIndex.clear();
        loadStationIndex.clear();
        portableIndex.clear();

        config.forEach((playerId, byDim) ->
                byDim.forEach((dimId, byPos) ->
                        byPos.forEach((pos, data) -> {
                            if (data.enabled()) {
                                if (data.mode() == ContainerTrackingMode.LOAD_STATION) {
                                    indexAdd(loadStationIndex, playerId, dimId, pos);
                                } else if (data.mode() == ContainerTrackingMode.CONTAINER) {
                                    indexAdd(enabledIndex, playerId, dimId, pos);
                                }
                            }
                        })));

        portableContainers.forEach((playerId, containerIds) ->
                containerIds.forEach(containerId -> portableIndexAdd(playerId, containerId)));
    }

    private void indexAdd(Map<Identifier, Map<BlockPos, Set<UUID>>> targetIndex, UUID playerId, Identifier dimId, BlockPos pos) {
        targetIndex
                .computeIfAbsent(dimId, k -> new HashMap<>())
                .computeIfAbsent(pos, k -> new HashSet<>())
                .add(playerId);
    }

    private void indexRemove(Map<Identifier, Map<BlockPos, Set<UUID>>> targetIndex, UUID playerId, Identifier dimId, BlockPos pos) {
        var byPos = targetIndex.get(dimId);
        if (byPos == null) return;
        var owners = byPos.get(pos);
        if (owners == null) return;
        owners.remove(playerId);
        if (owners.isEmpty()) byPos.remove(pos);
        if (byPos.isEmpty()) targetIndex.remove(dimId);
    }

    private void portableIndexAdd(UUID playerId, UUID containerId) {
        portableIndex.computeIfAbsent(containerId, k -> new HashSet<>()).add(playerId);
    }

    private void portableIndexRemove(UUID playerId, UUID containerId) {
        var owners = portableIndex.get(containerId);
        if (owners == null) return;
        owners.remove(playerId);
        if (owners.isEmpty()) portableIndex.remove(containerId);
    }

    public static final Codec<AFKContainerConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codecs.UUID_CODEC,
                            Codec.unboundedMap(Identifier.CODEC,
                                    Codec.unboundedMap(Codecs.STRING_BLOCK_POS_CODEC, ContainerData.CODEC))
                    ).fieldOf("config").forGetter(AFKContainerConfigData::getConfig),
                    Codec.unboundedMap(Codecs.UUID_CODEC,
                            Codecs.UUID_CODEC.listOf()
                    ).optionalFieldOf("portable_containers", Map.of()).forGetter(AFKContainerConfigData::getPortableContainersForCodec)
            )
            .apply(instance, AFKContainerConfigData::new));

    public static final SavedDataType<AFKContainerConfigData> TYPE = new SavedDataType<>(ID, AFKContainerConfigData::new, CODEC, null);
    private static final SavedDataType<AFKContainerConfigData> LEGACY_TYPE = new SavedDataType<>(LEGACY_ID, AFKContainerConfigData::new, CODEC, null);

    // Almacén global: lo anclamos al data storage del overworld.
    public static AFKContainerConfigData getConfig(MinecraftServer server) {
        if (server == null) {
            return new AFKContainerConfigData();
        }
        var storage = server.overworld().getDataStorage();
        var current = storage.get(TYPE);
        if (current != null) {
            return current;
        }
        var legacy = storage.get(LEGACY_TYPE);
        if (legacy != null) {
            var migrated = new AFKContainerConfigData(legacy.getConfig(), legacy.getPortableContainersForCodec());
            migrated.setDirty();
            storage.set(TYPE, migrated);
            log.info("Migrated legacy AFKContainerConfigData from {} to {}", LEGACY_ID, ID);
            return migrated;
        }
        return storage.computeIfAbsent(TYPE);
    }

    public void set(ServerPlayer player, BlockPos pos, boolean active) {
        setPositionalMode(player, pos, active ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF);
    }

    public void setPositionalMode(ServerPlayer player, BlockPos pos, ContainerTrackingMode mode) {
        var playerId = player.getGameProfile().id();
        var dimensionId = player.level().dimension().identifier();

        this.config.computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(dimensionId, k -> new HashMap<>());

        indexRemove(enabledIndex, playerId, dimensionId, pos);
        indexRemove(loadStationIndex, playerId, dimensionId, pos);

        if (mode == null || mode == ContainerTrackingMode.OFF) {
            this.config.get(playerId).get(dimensionId).remove(pos);
        } else {
            this.config.get(playerId).get(dimensionId).put(pos, new ContainerData(true, mode));
            if (mode == ContainerTrackingMode.LOAD_STATION) {
                indexAdd(loadStationIndex, playerId, dimensionId, pos);
            } else if (mode == ContainerTrackingMode.CONTAINER) {
                indexAdd(enabledIndex, playerId, dimensionId, pos);
            }
        }
        setDirty();
    }

    public void setPortableContainer(ServerPlayer player, UUID containerId, boolean active) {
        var playerId = player.getGameProfile().id();
        if (active) {
            this.portableContainers.computeIfAbsent(playerId, k -> new HashSet<>()).add(containerId);
            portableIndexAdd(playerId, containerId);
        } else {
            var set = this.portableContainers.get(playerId);
            if (set != null) {
                set.remove(containerId);
                if (set.isEmpty()) {
                    this.portableContainers.remove(playerId);
                }
            }
            portableIndexRemove(playerId, containerId);
        }
        setDirty();
    }

    public boolean isPortableContainerEnabled(UUID containerId) {
        var owners = portableIndex.get(containerId);
        return owners != null && !owners.isEmpty();
    }

    public boolean isPortableContainerEnabledFor(UUID playerId, UUID containerId) {
        var owners = portableIndex.get(containerId);
        return owners != null && owners.contains(playerId);
    }

    public Set<UUID> getPortableContainerOwners(UUID containerId) {
        return Set.copyOf(portableIndex.getOrDefault(containerId, Set.of()));
    }

    public ContainerData resolveEffectiveData(ServerPlayer player, BlockPos pos, BlockEntity blockEntity) {
        var playerId = player.getGameProfile().id();
        if (blockEntity != null) {
            Optional<UUID> containerId = PortableContainerIdentityHelper.getContainerId(blockEntity);
            if (containerId.isPresent() && isPortableContainerEnabledFor(playerId, containerId.get())) {
                return new ContainerData(ContainerTrackingMode.PORTABLE_CONTAINER);
            }
        }
        return getContainerData(player, pos)
                .filter(ContainerData::enabled)
                .orElseGet(() -> new ContainerData(ContainerTrackingMode.OFF));
    }

    public Optional<ContainerData> getContainerData(ServerPlayer serverPlayer, BlockPos pos) {
        var playerId = serverPlayer.getGameProfile().id();
        var dimensionId = serverPlayer.level().dimension().identifier();

        if (!this.config.containsKey(playerId)) {
            return Optional.empty();
        }

        if (!this.config.get(playerId).containsKey(dimensionId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.config.get(playerId).get(dimensionId).get(pos));
    }

    public void deleteContainerData(ServerPlayer player, BlockPos pos) {
        var playerId = player.getGameProfile().id();
        var dimensionId = player.level().dimension().identifier();
        if (!this.config.containsKey(playerId)) {
            return;
        }

        if (!this.config.get(playerId).containsKey(dimensionId)) {
            return;
        }

        this.config.get(playerId).get(dimensionId).remove(pos);
        indexRemove(enabledIndex, playerId, dimensionId, pos);
        indexRemove(loadStationIndex, playerId, dimensionId, pos);
        log.info("Container data has been deleted from the server for player {}", playerId);

        setDirty();
    }

    public void deleteFixedContainersAt(Identifier dimensionId, BlockPos pos) {
        var owners = getEnabledOwnersAt(dimensionId, pos);
        if (owners.isEmpty()) {
            return;
        }

        for (UUID playerId : owners) {
            var byDim = this.config.get(playerId);
            if (byDim != null) {
                var byPos = byDim.get(dimensionId);
                if (byPos != null) {
                    var current = byPos.get(pos);
                    if (current == null || current.mode() == ContainerTrackingMode.CONTAINER) {
                        byPos.remove(pos);
                    }
                }
            }
            indexRemove(enabledIndex, playerId, dimensionId, pos);
        }
        log.info("Fixed container data at {} in {} has been deleted for all owners", pos, dimensionId);
        setDirty();
    }

    public boolean isContainerEnabledIn(ServerPlayer player, List<BlockPos> positions) {
        return positions.stream()
                .anyMatch(pos -> getContainerData(player, pos).isPresent()
                        && getContainerData(player, pos).get().enabled()
                        && getContainerData(player, pos).get().mode() == ContainerTrackingMode.CONTAINER);
    }

    public boolean isContainerEnabledAt(Identifier dimensionId, BlockPos pos) {
        var byPos = enabledIndex.get(dimensionId);
        if (byPos == null) return false;
        var owners = byPos.get(pos);
        return owners != null && !owners.isEmpty();
    }

    public Set<UUID> getEnabledOwnersAt(Identifier dimensionId, BlockPos pos) {
        var byPos = enabledIndex.get(dimensionId);
        if (byPos == null) return Set.of();
        return Set.copyOf(byPos.getOrDefault(pos, Set.of()));
    }

    public boolean isLoadStationEnabledAt(Identifier dimensionId, BlockPos pos) {
        var byPos = loadStationIndex.get(dimensionId);
        if (byPos == null) return false;
        var owners = byPos.get(pos);
        return owners != null && !owners.isEmpty();
    }

    public Set<UUID> getLoadStationOwnersAt(Identifier dimensionId, BlockPos pos) {
        var byPos = loadStationIndex.get(dimensionId);
        if (byPos == null) return Set.of();
        return Set.copyOf(byPos.getOrDefault(pos, Set.of()));
    }

    public Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> getConfig() {
        return config;
    }

    private Map<UUID, List<UUID>> getPortableContainersForCodec() {
        var result = new HashMap<UUID, List<UUID>>();
        this.portableContainers.forEach((playerId, ids) -> {
            if (ids != null && !ids.isEmpty()) {
                result.put(playerId, new ArrayList<>(ids));
            }
        });
        return result;
    }
}
