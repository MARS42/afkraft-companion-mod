package com.mars4two.ikang.core.snapshots.sphere;

import com.mars4two.ikang.core.snapshots.model.RawCategoryCap;
import com.mars4two.ikang.core.snapshots.model.RawMobCluster;
import com.mars4two.ikang.core.snapshots.model.RawMobSphere;
import com.mars4two.ikang.core.snapshots.model.RawMobTypeBreakdown;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSphereDomainCalculatorTest {

    @Test
    void computesCapsSphereZonesAndSectionClustersMatchingVanillaThresholds() {
        MobSphereDomainCalculator.WorldContext world = new MobSphereDomainCalculator.WorldContext(
                "minecraft:overworld",
                0.0,
                64.0,
                0.0,
                -64,
                320,
                289,
                0,
                64,
                0,
                false
        );

        List<MobSphereDomainCalculator.CategorySpec> specs = List.of(
                new MobSphereDomainCalculator.CategorySpec(0, "monster", 70, 128, 32, 45),
                new MobSphereDomainCalculator.CategorySpec(1, "creature", 10, 128, 32, 8)
        );

        List<MobSphereDomainCalculator.MobSample> mobs = List.of(
                // Inside 24m exclusion (distance = 10): cap-counted, neither immediate nor despawn zone
                new MobSphereDomainCalculator.MobSample("minecraft:zombie", 0, 10.0, 64.0, 0.0, 10, 64, 0, false),
                // Inside 24..32m immediate zone (distance = 28): cap-counted + immediate zone
                new MobSphereDomainCalculator.MobSample("minecraft:skeleton", 0, 28.0, 64.0, 0.0, 28, 64, 0, false),
                // Same section (1, 4, 0 -> blocks 16..31) to verify lexicographic tie-breaking (creeper < skeleton)
                new MobSphereDomainCalculator.MobSample("minecraft:creeper", 0, 29.0, 64.0, 0.0, 29, 64, 0, false),
                // Inside 32..128m despawn zone (distance = 100): cap-counted + despawn zone
                new MobSphereDomainCalculator.MobSample("minecraft:zombie", 0, 100.0, 64.0, 0.0, 100, 64, 0, false),
                // Persistent mob at distance 30: excluded from capCount, counted in persistentCount
                new MobSphereDomainCalculator.MobSample("minecraft:zombie", 0, 30.0, 64.0, 0.0, 30, 64, 0, true),
                // High-altitude mob (dy = 200 -> 3D dist > 128, but horizontal chunk center < 128): still counts in local cap!
                new MobSphereDomainCalculator.MobSample("minecraft:zombie", 0, 8.0, 264.0, 8.0, 8, 264, 8, false),
                // Far mob outside both 128m sphere and 128m horizontal cylinder: completely ignored
                new MobSphereDomainCalculator.MobSample("minecraft:zombie", 0, 200.0, 64.0, 200.0, 200, 64, 200, false)
        );

        RawMobSphere sphere = MobSphereDomainCalculator.calculate(world, specs, mobs);

        RawCategoryCap monsterCap = sphere.categories().get(0);
        assertEquals("monster", monsterCap.category());
        assertEquals(5, monsterCap.localCount()); // 5 non-persistent monsters inside horizontal cylinder
        assertEquals(70, monsterCap.localMax());
        assertEquals(45, monsterCap.globalCount());
        assertEquals(70, monsterCap.globalMax()); // 70 * 289 / 289

        RawMobTypeBreakdown zombieBreakdown = sphere.breakdown().get(0);
        assertEquals("minecraft:zombie", zombieBreakdown.entityType());
        assertEquals(3, zombieBreakdown.capCount());
        assertEquals(1, zombieBreakdown.persistentCount());
        assertEquals(0, zombieBreakdown.inImmediateZone());
        assertEquals(1, zombieBreakdown.inDespawnZone());

        // Section (1, 4, 0) holds skeleton (1), creeper (1), persistent zombie (1) -> tie at 1 each, creeper wins lexicographically
        RawMobCluster sectionCluster = sphere.clusters().stream()
                .filter(c -> c.sectionX() == 1 && c.sectionY() == 4 && c.sectionZ() == 0)
                .findFirst()
                .orElseThrow();
        assertEquals(2, sectionCluster.capCount());
        assertEquals(1, sectionCluster.persistentCount());
        assertEquals("minecraft:creeper", sectionCluster.dominantType());

        // Spectator mode skips local scanning while preserving global category caps
        MobSphereDomainCalculator.WorldContext spectatorWorld = new MobSphereDomainCalculator.WorldContext(
                "minecraft:overworld", 0.0, 64.0, 0.0, -64, 320, 289, null, null, null, true
        );
        RawMobSphere spectatorSphere = MobSphereDomainCalculator.calculate(spectatorWorld, specs, mobs);
        assertEquals(0, spectatorSphere.categories().get(0).localCount());
        assertEquals(45, spectatorSphere.categories().get(0).globalCount());
        assertTrue(spectatorSphere.breakdown().isEmpty());
        assertTrue(spectatorSphere.clusters().isEmpty());
    }
}
