package com.mars4two.ikang.core.util;

import com.mars4two.ikang.core.snapshots.model.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Computes the local & global mobcap, creature breakdown by type, and 16x16x16 SectionPos
 * spatial clusters within a player's 128-block spawning sphere.
 */
public final class MobSphereCalculator {

    private static final int MAGIC_NUMBER = 289; // 17 * 17
    private static final double LOCAL_CHUNK_RADIUS_SQR = 16384.0; // 128^2 horizontal chunk center distance
    private static final double MIN_SPAWN_DIST_SQR = 576.0; // 24^2 exclusion sphere

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

        RawSpawnGeometry geometry = new RawSpawnGeometry(
                dimension,
                Math.round(playerPos.x * 100.0) / 100.0,
                Math.round(playerPos.y * 100.0) / 100.0,
                Math.round(playerPos.z * 100.0) / 100.0,
                level.getMinY(),
                level.getMaxY(),
                spawnableChunkCount,
                worldSpawnX,
                worldSpawnY,
                worldSpawnZ
        );

        EnumMap<MobCategory, Integer> localCategoryCounts = new EnumMap<>(MobCategory.class);
        for (MobCategory cat : TRACKED_CATEGORIES) {
            localCategoryCounts.put(cat, 0);
        }

        Map<String, TypeAccumulator> typeMap = new HashMap<>();
        Map<Long, ClusterAccumulator> clusterMap = new HashMap<>();

        if (!player.isSpectator()) {
            AABB searchBox = AABB.ofSize(playerPos, 256.0, 256.0, 256.0);
            List<Entity> nearbyEntities = level.getEntities((Entity) null, searchBox,
                    e -> e.getType().getCategory() != MobCategory.MISC);

            for (Entity entity : nearbyEntities) {
                MobCategory category = entity.getType().getCategory();
                boolean isPersistent = entity instanceof Mob mob
                        && (mob.isPersistenceRequired() || mob.requiresCustomPersistence());

                BlockPos blockPos = entity.blockPosition();
                ChunkPos chunkPos = ChunkPos.containing(blockPos);

                // Vanilla local mobcap check: horizontal distance from player to chunk center < 128 blocks
                double chunkCenterX = SectionPos.sectionToBlockCoord(chunkPos.x(), 8);
                double chunkCenterZ = SectionPos.sectionToBlockCoord(chunkPos.z(), 8);
                double dxChunk = chunkCenterX - playerPos.x;
                double dzChunk = chunkCenterZ - playerPos.z;
                boolean inLocalCapCylinder = (dxChunk * dxChunk + dzChunk * dzChunk) < LOCAL_CHUNK_RADIUS_SQR;

                if (inLocalCapCylinder && !isPersistent && entity instanceof Mob) {
                    localCategoryCounts.merge(category, 1, Integer::sum);
                }

                // 3D sphere distance check for breakdown and interactive 3D visualization
                double distSqr3D = playerPos.distanceToSqr(entity.position());
                int despawnDist = category.getDespawnDistance();
                double maxSphereSqr = (double) despawnDist * despawnDist;

                if (distSqr3D <= maxSphereSqr || inLocalCapCylinder) {
                    String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                    TypeAccumulator typeAcc = typeMap.computeIfAbsent(typeId,
                            k -> new TypeAccumulator(typeId, category.getName()));

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

                    int secX = SectionPos.blockToSectionCoord(blockPos.getX());
                    int secY = SectionPos.blockToSectionCoord(blockPos.getY());
                    int secZ = SectionPos.blockToSectionCoord(blockPos.getZ());
                    long sectionKey = SectionPos.asLong(secX, secY, secZ);

                    ClusterAccumulator clusterAcc = clusterMap.computeIfAbsent(sectionKey, k -> {
                        double centerSecX = SectionPos.sectionToBlockCoord(secX, 8);
                        double centerSecY = SectionPos.sectionToBlockCoord(secY, 8);
                        double centerSecZ = SectionPos.sectionToBlockCoord(secZ, 8);
                        int distBlocks = (int) Math.round(Math.sqrt(playerPos.distanceToSqr(centerSecX, centerSecY, centerSecZ)));
                        return new ClusterAccumulator(secX, secY, secZ, category.getName(), distBlocks);
                    });
                    clusterAcc.add(typeId, isPersistent);
                }
            }
        }

        List<RawCategoryCap> categories = new ArrayList<>(TRACKED_CATEGORIES.length);
        for (MobCategory cat : TRACKED_CATEGORIES) {
            int localCount = localCategoryCounts.getOrDefault(cat, 0);
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

        List<RawMobTypeBreakdown> breakdown = typeMap.values().stream()
                .sorted(Comparator.comparingInt((TypeAccumulator a) -> a.capCount).reversed()
                        .thenComparing(a -> a.entityType))
                .map(TypeAccumulator::toRaw)
                .toList();

        List<RawMobCluster> clusters = clusterMap.values().stream()
                .sorted(Comparator.comparingInt((ClusterAccumulator c) -> c.capCount).reversed()
                        .thenComparingInt(c -> c.sectionX)
                        .thenComparingInt(c -> c.sectionY)
                        .thenComparingInt(c -> c.sectionZ))
                .map(ClusterAccumulator::toRaw)
                .toList();

        return new RawMobSphere(geometry, categories, breakdown, clusters);
    }

    private static final class TypeAccumulator {
        final String entityType;
        final String category;
        int capCount;
        int persistentCount;
        int inImmediateZone;
        int inDespawnZone;

        TypeAccumulator(String entityType, String category) {
            this.entityType = entityType;
            this.category = category;
        }

        RawMobTypeBreakdown toRaw() {
            return new RawMobTypeBreakdown(
                    entityType,
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
        final Map<String, Integer> typeCounts = new HashMap<>();

        ClusterAccumulator(int sectionX, int sectionY, int sectionZ, String category, int distanceToPlayer) {
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.category = category;
            this.distanceToPlayer = distanceToPlayer;
        }

        void add(String typeId, boolean isPersistent) {
            if (isPersistent) {
                persistentCount++;
            } else {
                capCount++;
            }
            typeCounts.merge(typeId, 1, Integer::sum);
        }

        RawMobCluster toRaw() {
            String dominantType = typeCounts.entrySet().stream()
                    .max(Map.Entry.<String, Integer>comparingByValue()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey)
                    .orElse("");
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
