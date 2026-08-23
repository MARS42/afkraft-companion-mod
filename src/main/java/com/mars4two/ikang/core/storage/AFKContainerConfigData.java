package com.mars4two.ikang.core.storage;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.util.Codecs;
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
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AFKContainerConfigData extends SavedData {

    public static final Logger log = LoggerFactory.getLogger("AFKContainerConfigData");

    private static final Identifier ID = AFKraftCompanion.id("afk_container_config_data");

    private final HashMap<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> config;

    // Índice inverso derivado (NO se serializa): dimensión -> pos -> jugadores activos.
    private final transient Map<Identifier, Map<BlockPos, Set<UUID>>> enabledIndex = new HashMap<>();

    public record ContainerData(boolean enabled) implements CustomPacketPayload {

        public static final Identifier ID = AFKraftCompanion.id("afk_container_data_payload");

        public static final Type<ContainerData> PAYLOAD_TYPE = new Type<>(ID);

        public static final Codec<ContainerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.fieldOf("enabled").forGetter(ContainerData::enabled)
                )
                .apply(instance, ContainerData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf,ContainerData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                ContainerData::enabled,
                ContainerData::new
        );

        @Override
        public Type<ContainerData> type() {
            return PAYLOAD_TYPE;
        }
    }

    public AFKContainerConfigData() {
        this.config = new HashMap<>();
    }

    public AFKContainerConfigData(Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> afkContainerConfigData) {
        var deepMutableCopy = new HashMap<UUID, Map<Identifier, Map<BlockPos, ContainerData>>>();
        afkContainerConfigData.forEach((k, v) -> {
            deepMutableCopy.put(k, new HashMap<>(v));
            v.forEach((k1, v1) -> {
                deepMutableCopy.get(k).put(k1, new HashMap<>(v1));
                v1.forEach((k2, v2) -> deepMutableCopy.get(k).get(k1).put(k2, v2));
            });
        });

        this.config = deepMutableCopy;
        rebuildIndex();
    }

    private void rebuildIndex() {
        enabledIndex.clear();
        config.forEach((playerId, byDim) ->
                byDim.forEach((dimId, byPos) ->
                        byPos.forEach((pos, data) -> {
                            if (data.enabled()) {
                                indexAdd(playerId, dimId, pos);
                            }
                        })));
    }

    private void indexAdd(UUID playerId, Identifier dimId, BlockPos pos) {
        enabledIndex
                .computeIfAbsent(dimId, k -> new HashMap<>())
                .computeIfAbsent(pos, k -> new HashSet<>())
                .add(playerId);
    }

    private void indexRemove(UUID playerId, Identifier dimId, BlockPos pos) {
        var byPos = enabledIndex.get(dimId);
        if (byPos == null) return;
        var owners = byPos.get(pos);
        if (owners == null) return;
        owners.remove(playerId);
        if (owners.isEmpty()) byPos.remove(pos);
        if (byPos.isEmpty()) enabledIndex.remove(dimId);
    }

    public static final Codec<AFKContainerConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codecs.UUID_CODEC,
                    Codec.unboundedMap(Identifier.CODEC,
                            Codec.unboundedMap(Codecs.STRING_BLOCK_POS_CODEC, ContainerData.CODEC))
                    ).fieldOf("config").forGetter(AFKContainerConfigData::getConfig)
            )
            .apply(instance, AFKContainerConfigData::new));

    public static final SavedDataType<AFKContainerConfigData> TYPE = new SavedDataType<>(ID, AFKContainerConfigData::new, CODEC, null);

    // Almacén global: lo anclamos al data storage del overworld.
    public static AFKContainerConfigData getConfig(MinecraftServer server) {

        if (server == null) {
            return new AFKContainerConfigData();
        }

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void set(ServerPlayer player, BlockPos pos, boolean active) {
        var playerId = player.getGameProfile().id();
        var dimensionId = player.level().dimension().identifier();

        if(!this.config.containsKey(playerId)) {
            this.config.put(playerId, new HashMap<>());
        }

        if(!this.config.get(playerId).containsKey(dimensionId)) {
            this.config.get(playerId).put(dimensionId, new HashMap<>());
        }

        this.config.get(playerId).get(dimensionId).put(pos, new ContainerData(active));

        if (active) {
            indexAdd(playerId, dimensionId, pos);
        } else {
            indexRemove(playerId, dimensionId, pos);
        }
        setDirty();
    }

    public Optional<ContainerData> getContainerData(ServerPlayer serverPlayer, BlockPos pos) {
        var playerId = serverPlayer.getGameProfile().id();
        var dimensionId = serverPlayer.level().dimension().identifier();

        if(!this.config.containsKey(playerId)) {
            return Optional.empty();
        }

        if(!this.config.get(playerId).containsKey(dimensionId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.config.get(playerId).get(dimensionId).get(pos));
    }

    public void deleteContainerData(ServerPlayer player, BlockPos pos) {
        var playerId = player.getGameProfile().id();
        var dimensionId = player.level().dimension().identifier();
        if(!this.config.containsKey(playerId)) {
            return;
        }

        if(!this.config.get(playerId).containsKey(dimensionId)) {
            return;
        }

        this.config.get(playerId).get(dimensionId).remove(pos);
        indexRemove(playerId, dimensionId, pos);
        log.info("Container data has been deleted from the server for player {}", playerId);

        setDirty();
    }

    public boolean isContainerEnabledIn(ServerPlayer player, List<BlockPos> positions) {
        return positions.stream()
                .anyMatch(pos -> getContainerData(player, pos).isPresent() && getContainerData(player, pos).get().enabled());
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

    public Map<UUID, Map<Identifier, Map<BlockPos, ContainerData>>> getConfig() {
        return config;
    }
}
