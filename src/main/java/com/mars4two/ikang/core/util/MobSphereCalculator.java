package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
import java.util.Comparator;
import java.util.List;

/**
 * Computes the local & global mobcap, creature breakdown by type, and 16x16x16 SectionPos
 * spatial clusters within a player's 128-block spawning sphere.
 *
 * Optimized for zero boxing and minimal allocations on the server thread:
 * - Uses EntityTypeTest.forClass(Mob.class) so EntitySectionStorage skips non-Mob lists (items, XP orbs, arrows).
 * - Uses fastutil primitive/reference maps (Reference2ObjectOpenHashMap, Long2ObjectOpenHashMap) to avoid boxing.
 * - Resolves BuiltInRegistries.ENTITY_TYPE String IDs only once per unique EntityType instead of per entity.
 */
public final class MobSphereCalculator {

    private static final int MAGIC_NUMBER = 289; // 17 * 17
    private static final double LOCAL_CHUNK_RADIUS_SQR = 16384.0; // 128^2 horizontal chunk center distance
    private static final double MIN_SPAWN_DIST_SQR = 576.0; // 24^2 exclusion sphere

    private static final EntityTypeTest< net.minecraft.world.entity.Entity, Mob> MOB_TYPE_TEST =
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

        RawSpawnGeometry geometry = new RawSpawnGeometry(
                dimension,
                Math.round(px * 100.0) / 100.0,
                Math.round(py * 100.0) / 100.0,
                Math.round(pz * 100.0) / 100.0,
                minY,
                maxY,
                spawnableChunkCount,
                worldSpawnX,
                worldSpawnY,
                worldSpawnZ
        );

        int[] localCategoryCounts = new int[MobCategory.values().length];
        Reference2ObjectOpenHashMap<EntityType<?>, TypeAccumulator> typeMap = new Reference2ObjectOpenHashMap<>(16);
        Long2ObjectOpenHashMap<ClusterAccumulator> clusterMap = new Long2ObjectOpenHashMap<>(32);

        if (!player.isSpectator()) {
            // Clamp vertical AABB to world height boundaries so we don't scan empty sections above/below the world
            double minBoxY = Math.max(minY, py - 128.0);
            double maxBoxY = Math.min(maxY, py + 128.0);
            AABB searchBox = new AABB(px - 128.0, minBoxY, pz - 128.0, px + 128.0, maxBoxY, pz + 128.0);

            List<Mob> nearbyMobs = level.getEntities(MOB_TYPE_TEST, searchBox,
                    mob -> mob.getType().getCategory() != MobCategory.MISC);

            for (int i = 0, size = nearbyMobs.size(); i < size; i++) {
                Mob mob = nearbyMobs.get(i);
                EntityType<?> entityType = mob.getType();
                MobCategory category = entityType.getCategory();
                boolean isPersistent = mob.isPersistenceRequired() || mob.requiresCustomPersistence();

                int bx = mob.getBlockX();
                int by = mob.getBlockY();
                int bz = mob.getBlockZ();

                int secX = SectionPos.blockToSectionCoord(bx);
                int secZ = SectionPos.blockToSectionCoord(bz);

                // Vanilla local mobcap check: horizontal distance from player to chunk center < 128 blocks
                double chunkCenterX = (secX << 4) + 8.0;
                double chunkCenterZ = (secZ << 4) + 8.0;
                double dxChunk = chunkCenterX - px;
                double dzChunk = chunkCenterZ - pz;
                boolean inLocalCapCylinder = (dxChunk * dxChunk + dzChunk * dzChunk) < LOCAL_CHUNK_RADIUS_SQR;

                if (inLocalCapCylinder && !isPersistent) {
                    localCategoryCounts[category.ordinal()]++;
                }

                // 3D sphere distance check for breakdown and interactive 3D visualization
                double dx = mob.getX() - px;
                double dy = mob.getY() - py;
                double dz = mob.getZ() - pz;
                double distSqr3D = dx * dx + dy * dy + dz * dz;

                int despawnDist = category.getDespawnDistance();
                double maxSphereSqr = (double) despawnDist * despawnDist;

                if (distSqr3D <= maxSphereSqr || inLocalCapCylinder) {
                    TypeAccumulator typeAcc = typeMap.get(entityType);
                    if (typeAcc == null) {
                        typeAcc = new TypeAccumulator(entityType, category.getName());
                        typeMap.put(entityType, typeAcc);
                    }

                    int noDespawnDist = category.getNoDespawnDistance();
                    double noDespawnSqr = (double) noDespawnDist * noDespawnDist;

                    if (isPersistent) {
                        typeAcc.persistentCount++;
                    } else {
                        typeAcc.capCount++;
                        if (distSqr3D > MIN_SPAWN_DIST_SQR && distSqr3D <= noDespawnSqr) {
                            typeAcc.inImmediateZone++;
                        } else if (distSqr3D > noDespawnSqr && distSqr3D <= maxSphereSqr) {
                            typeAcc.inDespawnZone++;
                        }
                    }

                    int secY = SectionPos.blockToSectionCoord(by);
                    long sectionKey = SectionPos.asLong(secX, secY, secZ);

                    ClusterAccumulator clusterAcc = clusterMap.get(sectionKey);
                    if (clusterAcc == null) {
                        double centerSecY = (secY << 4) + 8.0;
                        double dSecY = centerSecY - py;
                        int distBlocks = (int) Math.round(Math.sqrt(dxChunk * dxChunk + dSecY * dSecY + dzChunk * dzChunk));
                        clusterAcc = new ClusterAccumulator(secX, secY, secZ, category.getName(), distBlocks);
                        clusterMap.put(sectionKey, clusterAcc);
                    }
                    clusterAcc.add(entityType, isPersistent);
                }
            }
        }

        // Resolve EntityType registry strings once per distinct EntityType
        Reference2ObjectOpenHashMap<EntityType<?>, String> resolvedNames = new Reference2ObjectOpenHashMap<>(typeMap.size());
        for (EntityType<?> type : typeMap.keySet()) {
            resolvedNames.put(type, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        }

        List<RawCategoryCap> categories = new ArrayList<>(TRACKED_CATEGORIES.length);
        for (MobCategory cat : TRACKED_CATEGORIES) {
            int localCount = localCategoryCounts[cat.ordinal()];
            int localMax = cat.getMaxInstancesPerChunk();
            int globalCount = globalCounts != null ? globalCounts.getInt(cat) : 0;
            int globalMax = localMax * spawnableChunkCount / MAGIC_NUMBER;
            categories.add(new RawCategoryCap(
                    cat.getName(),
                    localCount,
                    localMax,
                    globalCount,
                    globalMax,
                    cat.getDespawnDistance()
            ));
        }

        List<RawMobTypeBreakdown> breakdown = new ArrayList<>(typeMap.size());
        for (TypeAccumulator acc : typeMap.values()) {
            breakdown.add(acc.toRaw(resolvedNames.get(acc.entityType)));
        }
        breakdown.sort(Comparator.comparingInt(RawMobTypeBreakdown::capCount).reversed()
                .thenComparing(RawMobTypeBreakdown::entityType));

        List<RawMobCluster> clusters = new ArrayList<>(clusterMap.size());
        for (ClusterAccumulator acc : clusterMap.values()) {
            clusters.add(acc.toRaw(resolvedNames));
        }
        clusters.sort(Comparator.comparingInt(RawMobCluster::capCount).reversed()
                .thenComparingInt(RawMobCluster::sectionX)
                .thenComparingInt(RawMobCluster::sectionY)
                .thenComparingInt(RawMobCluster::sectionZ));

        return new RawMobSphere(geometry, categories, breakdown, clusters);
    }

    private static final class TypeAccumulator {
        final EntityType<?> entityType;
        final String category;
        int capCount;
        int persistentCount;
        int inImmediateZone;
        int inDespawnZone;

        TypeAccumulator(EntityType<?> entityType, String category) {
            this.entityType = entityType;
            this.category = category;
        }

        RawMobTypeBreakdown toRaw(String resolvedName) {
            return new RawMobTypeBreakdown(
                    resolvedName,
                    category,
                    capCount,
                    persistentCount,
                    inImmediateZone,
                    inDespawnZone
            );
        }
    }

    private static final class ClusterAccumulator {
        final int sectionX;
        final int sectionY;
        final int sectionZ;
        final String category;
        final int distanceToPlayer;
        int capCount;
        int persistentCount;
        final Reference2IntOpenHashMap<EntityType<?>> typeCounts = new Reference2IntOpenHashMap<>(4);

        ClusterAccumulator(int sectionX, int sectionY, int sectionZ, String category, int distanceToPlayer) {
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.category = category;
            this.distanceToPlayer = distanceToPlayer;
        }

        void add(EntityType<?> type, boolean isPersistent) {
            if (isPersistent) {
                persistentCount++;
            } else {
                capCount++;
            }
            typeCounts.addTo(type, 1);
        }

        RawMobCluster toRaw(Reference2ObjectOpenHashMap<EntityType<?>, String> resolvedNames) {
            String dominantType = "";
            int maxCount = -1;
            for (var entry : typeCounts.reference2IntEntrySet()) {
                int count = entry.getIntValue();
                String name = resolvedNames.getOrDefault(entry.getKey(), "");
                if (count > maxCount || (count == maxCount && name.compareTo(dominantType) < 0)) {
                    maxCount = count;
                    dominantType = name;
                }
            }
            return new RawMobCluster(
                    sectionX,
                    sectionY,
                    sectionZ,
                    category,
                    dominantType,
                    capCount,
                    persistentCount,
                    distanceToPlayer
            );
        }
    }
}
