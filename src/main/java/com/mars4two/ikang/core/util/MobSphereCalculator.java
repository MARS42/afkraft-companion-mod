package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.sphere.MobSphereDomainCalculator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minecraft world adapter that probes nearby {@link Mob} entities and chunk spawn state,
 * delegating pure mobcap and 16x16x16 section clustering calculations to {@link MobSphereDomainCalculator}.
 *
 * Optimized for minimal overhead on the server thread:
 * - Uses EntityTypeTest.forClass(Mob.class) so EntitySectionStorage skips non-Mob lists (items, XP orbs, arrows).
 * - Resolves BuiltInRegistries.ENTITY_TYPE String IDs only once per unique EntityType instead of per entity.
 */
public final class MobSphereCalculator {

    private static final EntityTypeTest<net.minecraft.world.entity.Entity, Mob> MOB_TYPE_TEST =
            EntityTypeTest.forClass(Mob.class);

    private static final MobCategory[] TRACKED_CATEGORIES = Arrays.stream(MobCategory.values())
            .filter(c -> c != MobCategory.MISC)
            .toArray(MobCategory[]::new);

    private MobSphereCalculator() {
    }

    public static RawMobSphere calculate(ServerPlayer player) {
        ServerLevel level = player.level();
        ServerChunkCache chunkCache = level.getChunkSource();
        NaturalSpawner.SpawnState lastSpawnState = chunkCache.getLastSpawnState();

        int spawnableChunkCount = lastSpawnState != null
                ? lastSpawnState.getSpawnableChunkCount()
                : chunkCache.chunkMap.getDistanceManager().getNaturalSpawnChunkCount();

        Object2IntMap<MobCategory> globalCounts = lastSpawnState != null
                ? lastSpawnState.getMobCategoryCounts()
                : null;

        Vec3 playerPos = player.position();
        double px = playerPos.x;
        double py = playerPos.y;
        double pz = playerPos.z;
        String dimension = level.dimension().identifier().toString();

        LevelData.RespawnData respawnData = level.getRespawnData();
        Integer worldSpawnX = null;
        Integer worldSpawnY = null;
        Integer worldSpawnZ = null;
        if (respawnData != null && respawnData.dimension().equals(level.dimension())) {
            BlockPos spawnPos = respawnData.pos();
            worldSpawnX = spawnPos.getX();
            worldSpawnY = spawnPos.getY();
            worldSpawnZ = spawnPos.getZ();
        }

        int minY = level.getMinY();
        int maxY = level.getMaxY();
        boolean spectator = player.isSpectator();

        MobSphereDomainCalculator.WorldContext worldContext = new MobSphereDomainCalculator.WorldContext(
                dimension,
                px,
                py,
                pz,
                minY,
                maxY,
                spawnableChunkCount,
                worldSpawnX,
                worldSpawnY,
                worldSpawnZ,
                spectator
        );

        List<MobSphereDomainCalculator.CategorySpec> categorySpecs = new ArrayList<>(TRACKED_CATEGORIES.length);
        for (MobCategory cat : TRACKED_CATEGORIES) {
            int globalCount = globalCounts != null ? globalCounts.getInt(cat) : 0;
            categorySpecs.add(new MobSphereDomainCalculator.CategorySpec(
                    cat.ordinal(),
                    cat.getName(),
                    cat.getMaxInstancesPerChunk(),
                    cat.getDespawnDistance(),
                    cat.getNoDespawnDistance(),
                    globalCount
            ));
        }

        List<MobSphereDomainCalculator.MobSample> samples = List.of();
        if (!spectator) {
            // Clamp vertical AABB to world height boundaries so we don't scan empty sections above/below the world
            double minBoxY = Math.max(minY, py - 128.0);
            double maxBoxY = Math.min(maxY, py + 128.0);
            AABB searchBox = new AABB(px - 128.0, minBoxY, pz - 128.0, px + 128.0, maxBoxY, pz + 128.0);

            List<Mob> nearbyMobs = level.getEntities(MOB_TYPE_TEST, searchBox,
                    mob -> mob.getType().getCategory() != MobCategory.MISC);

            if (!nearbyMobs.isEmpty()) {
                Reference2ObjectOpenHashMap<EntityType<?>, String> resolvedNames = new Reference2ObjectOpenHashMap<>(16);
                samples = new ArrayList<>(nearbyMobs.size());
                for (int i = 0, size = nearbyMobs.size(); i < size; i++) {
                    Mob mob = nearbyMobs.get(i);
                    EntityType<?> entityType = mob.getType();
                    String typeId = resolvedNames.get(entityType);
                    if (typeId == null) {
                        typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
                        resolvedNames.put(entityType, typeId);
                    }
                    samples.add(new MobSphereDomainCalculator.MobSample(
                            typeId,
                            entityType.getCategory().ordinal(),
                            mob.getX(),
                            mob.getY(),
                            mob.getZ(),
                            mob.getBlockX(),
                            mob.getBlockY(),
                            mob.getBlockZ(),
                            mob.isPersistenceRequired() || mob.requiresCustomPersistence()
                    ));
                }
            }
        }

        return MobSphereDomainCalculator.calculate(worldContext, categorySpecs, samples);
    }
}
