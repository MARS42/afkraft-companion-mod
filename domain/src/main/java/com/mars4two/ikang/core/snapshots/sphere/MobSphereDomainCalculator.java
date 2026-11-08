package com.mars4two.ikang.core.snapshots.sphere;

import com.mars4two.ikang.core.snapshots.model.RawCategoryCap;
import com.mars4two.ikang.core.snapshots.model.RawMobCluster;
import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.model.RawMobTypeBreakdown;
import com.mars4two.ikang.core.snapshots.model.RawSpawnGeometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure domain calculator for local & global mobcap counts, creature breakdown by type,
 * and 16x16x16 section spatial clusters within a player's 128-block spawning sphere.
 */
public final class MobSphereDomainCalculator {

    public static final int MAGIC_NUMBER = 289; // 17 * 17
    public static final double LOCAL_CHUNK_RADIUS_SQR = 16384.0; // 128^2 horizontal chunk center distance
    public static final double MIN_SPAWN_DIST_SQR = 576.0; // 24^2 exclusion sphere

    public record WorldContext(
            String dimension,
            double playerX,
            double playerY,
            double playerZ,
            int minY,
            int maxY,
            int spawnableChunkCount,
            Integer worldSpawnX,
            Integer worldSpawnY,
            Integer worldSpawnZ,
            boolean spectator
    ) {
    }

    public record CategorySpec(
            int index,
            String name,
            int maxInstancesPerChunk,
            int despawnDistance,
            int noDespawnDistance,
            int globalCount
    ) {
    }

    public record MobSample(
            String entityTypeId,
            int categoryIndex,
            double x,
            double y,
            double z,
            int blockX,
            int blockY,
            int blockZ,
            boolean persistent
    ) {
    }

    private MobSphereDomainCalculator() {
    }

    public static RawMobSphere calculate(
            WorldContext world,
            List<CategorySpec> categorySpecs,
            List<MobSample> nearbyMobs
    ) {
        double px = world.playerX();
        double py = world.playerY();
        double pz = world.playerZ();

        RawSpawnGeometry geometry = new RawSpawnGeometry(
                world.dimension(),
                Math.round(px * 100.0) / 100.0,
                Math.round(py * 100.0) / 100.0,
                Math.round(pz * 100.0) / 100.0,
                world.minY(),
                world.maxY(),
                world.spawnableChunkCount(),
                world.worldSpawnX(),
                world.worldSpawnY(),
                world.worldSpawnZ()
        );

        int maxCategoryIndex = 0;
        for (CategorySpec spec : categorySpecs) {
            if (spec.index() + 1 > maxCategoryIndex) {
                maxCategoryIndex = spec.index() + 1;
            }
        }
        CategorySpec[] specsByIndex = new CategorySpec[maxCategoryIndex];
        for (CategorySpec spec : categorySpecs) {
            specsByIndex[spec.index()] = spec;
        }

        int[] localCategoryCounts = new int[maxCategoryIndex];
        Map<String, TypeAccumulator> typeMap = new HashMap<>(16);
        Map<Long, ClusterAccumulator> clusterMap = new HashMap<>(32);

        if (!world.spectator() && nearbyMobs != null) {
            for (int i = 0, size = nearbyMobs.size(); i < size; i++) {
                MobSample mob = nearbyMobs.get(i);
                int catIdx = mob.categoryIndex();
                if (catIdx < 0 || catIdx >= specsByIndex.length) {
                    continue;
                }
                CategorySpec category = specsByIndex[catIdx];
                if (category == null) {
                    continue;
                }

                boolean isPersistent = mob.persistent();
                int secX = blockToSectionCoord(mob.blockX());
                int secZ = blockToSectionCoord(mob.blockZ());

                // Vanilla local mobcap check: horizontal distance from player to chunk center < 128 blocks
                double chunkCenterX = (secX << 4) + 8.0;
                double chunkCenterZ = (secZ << 4) + 8.0;
                double dxChunk = chunkCenterX - px;
                double dzChunk = chunkCenterZ - pz;
                boolean inLocalCapCylinder = (dxChunk * dxChunk + dzChunk * dzChunk) < LOCAL_CHUNK_RADIUS_SQR;

                if (inLocalCapCylinder && !isPersistent) {
                    localCategoryCounts[catIdx]++;
                }

                // 3D sphere distance check for breakdown and interactive 3D visualization
                double dx = mob.x() - px;
                double dy = mob.y() - py;
                double dz = mob.z() - pz;
                double distSqr3D = dx * dx + dy * dy + dz * dz;

                int despawnDist = category.despawnDistance();
                double maxSphereSqr = (double) despawnDist * despawnDist;

                if (distSqr3D <= maxSphereSqr || inLocalCapCylinder) {
                    String entityTypeId = mob.entityTypeId();
                    TypeAccumulator typeAcc = typeMap.get(entityTypeId);
                    if (typeAcc == null) {
                        typeAcc = new TypeAccumulator(entityTypeId, category.name());
                        typeMap.put(entityTypeId, typeAcc);
                    }

                    int noDespawnDist = category.noDespawnDistance();
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

                    int secY = blockToSectionCoord(mob.blockY());
                    long sectionKey = packSectionKey(secX, secY, secZ);

                    ClusterAccumulator clusterAcc = clusterMap.get(sectionKey);
                    if (clusterAcc == null) {
                        double centerSecY = (secY << 4) + 8.0;
                        double dSecY = centerSecY - py;
                        int distBlocks = (int) Math.round(Math.sqrt(dxChunk * dxChunk + dSecY * dSecY + dzChunk * dzChunk));
                        clusterAcc = new ClusterAccumulator(secX, secY, secZ, category.name(), distBlocks);
                        clusterMap.put(sectionKey, clusterAcc);
                    }
                    clusterAcc.add(entityTypeId, isPersistent);
                }
            }
        }

        List<RawCategoryCap> categories = new ArrayList<>(categorySpecs.size());
        for (CategorySpec cat : categorySpecs) {
            int localCount = localCategoryCounts[cat.index()];
            int localMax = cat.maxInstancesPerChunk();
            int globalCount = cat.globalCount();
            int globalMax = localMax * world.spawnableChunkCount() / MAGIC_NUMBER;
            categories.add(new RawCategoryCap(
                    cat.name(),
                    localCount,
                    localMax,
                    globalCount,
                    globalMax,
                    cat.despawnDistance()
            ));
        }

        List<RawMobTypeBreakdown> breakdown = new ArrayList<>(typeMap.size());
        for (TypeAccumulator acc : typeMap.values()) {
            breakdown.add(acc.toRaw());
        }
        breakdown.sort(Comparator.comparingInt(RawMobTypeBreakdown::capCount).reversed()
                .thenComparing(RawMobTypeBreakdown::entityType));

        List<RawMobCluster> clusters = new ArrayList<>(clusterMap.size());
        for (ClusterAccumulator acc : clusterMap.values()) {
            clusters.add(acc.toRaw());
        }
        clusters.sort(Comparator.comparingInt(RawMobCluster::capCount).reversed()
                .thenComparingInt(RawMobCluster::sectionX)
                .thenComparingInt(RawMobCluster::sectionY)
                .thenComparingInt(RawMobCluster::sectionZ));

        return new RawMobSphere(geometry, categories, breakdown, clusters);
    }

    public static int blockToSectionCoord(int blockCoord) {
        return blockCoord >> 4;
    }

    private static long packSectionKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFL) << 42) | ((long) y & 0xFFFFFL) | (((long) z & 0x3FFFFFL) << 20);
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
        final Map<String, Integer> typeCounts = new HashMap<>(4);

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
            String dominantType = "";
            int maxCount = -1;
            for (var entry : typeCounts.entrySet()) {
                int count = entry.getValue();
                String name = entry.getKey();
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
