package com.mars4two.ikang.core.snapshots;

import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.snapshots.model.RawContainer;
import com.mars4two.ikang.core.snapshots.model.RawContainerType;
import com.mars4two.ikang.core.snapshots.model.RawEnchantment;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.snapshots.model.RawItemStackExtraData;
import com.mars4two.ikang.core.snapshots.model.RawPlayerEffect;
import com.mars4two.ikang.core.util.ContainerView;
import com.mars4two.ikang.core.util.MinecraftIdsUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotModelContractTest {

    @Test
    void blockCoordSerializesToExactBackendJsonKeysInsideDoubleChestPayload() {
        BlockCoord first = new BlockCoord(10, 64, -20);
        BlockCoord last = new BlockCoord(11, 64, -20);

        assertEquals("{\"x\":10,\"y\":64,\"z\":-20}", GsonConfig.toJson(first));

        RawContainerType doubleChest = RawContainerType.doubleChest(first, last);
        String json = GsonConfig.toJson(doubleChest.toPayload());

        assertTrue(json.contains("\"b.m:chest\""), "Expected aliased chest block id in payload JSON: " + json);
        assertTrue(json.contains("\"bicombiner\""), "Expected bicombiner key in payload JSON: " + json);
        assertTrue(json.contains("\"first\":{\"x\":10,\"y\":64,\"z\":-20}"), "Expected first BlockCoord JSON contract: " + json);
        assertTrue(json.contains("\"last\":{\"x\":11,\"y\":64,\"z\":-20}"), "Expected last BlockCoord JSON contract: " + json);
    }

    @Test
    void rawItemStackAndEffectAliasesMatchLegacyWireFormat() {
        assertEquals("e.m.haste", MinecraftIdsUtils.effectAlias("effect.minecraft.haste"));
        assertEquals("i.m:diamond_sword", MinecraftIdsUtils.itemAlias("minecraft:diamond_sword"));
        assertEquals("b.m:chest", MinecraftIdsUtils.blockAlias("minecraft:chest"));

        RawItemStack sword = RawItemStack.of(
                0,
                "minecraft:diamond_sword",
                false,
                null,
                1,
                List.of(new RawEnchantment("minecraft:sharpness", 5)),
                false
        );
        assertEquals("i.m:diamond_sword", sword.id());
        assertEquals("", sword.customName());

        RawItemStack stone = RawItemStack.of(
                2,
                "minecraft:stone",
                true,
                "Bricks",
                64,
                List.of(),
                true
        );
        assertEquals("b.m:stone", stone.id());
        assertEquals("Bricks", stone.customName());

        RawItemStack damagedSword = RawItemStack.of(
                1,
                "minecraft:diamond_sword",
                false,
                "Excalibur",
                1,
                List.of(),
                false,
                true,
                new RawItemStackExtraData(150, 1561)
        );
        assertEquals(true, damagedSword.isDamageable());
        assertEquals(1411, damagedSword.extraData().remainingUsages());
        assertArrayEquals(
                new Object[]{
                        "i.m:diamond_sword",
                        "Excalibur",
                        1,
                        new Object[0],
                        1,
                        false,
                        true,
                        new Object[]{150, 1561, 1411}
                },
                damagedSword.toPayload()
        );

        // Verify non-damageable backward-compatible wire format outputs "" for extraData
        assertArrayEquals(
                new Object[]{
                        "b.m:stone",
                        "Bricks",
                        64,
                        new Object[0],
                        2,
                        true,
                        false,
                        ""
                },
                stone.toPayload()
        );

        RawPlayerEffect haste = new RawPlayerEffect("effect.minecraft.haste", 1, 120, true, false);
        assertArrayEquals(new Object[]{"e.m.haste", 1, 120, true, false}, haste.toPayload());
    }

    @Test
    void containerAndLoadStationSnapshotsBuildExpectedPayloadsAndConflationKeys() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID portableId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        BlockCoord pos = new BlockCoord(-4, 70, 128);
        RawItemStack item = RawItemStack.of(0, "minecraft:iron_ingot", false, "", 32, List.of(), true);

        ContainerView view = new ContainerView() {
            @Override
            public RawContainerType type() {
                return RawContainerType.singleChest();
            }

            @Override
            public Optional<UUID> containerId() {
                return Optional.of(portableId);
            }

            @Override
            public BlockCoord blockCoord() {
                return pos;
            }

            @Override
            public String blockId() {
                return "minecraft:shulker_box";
            }

            @Override
            public String containerName() {
                return null;
            }

            @Override
            public List<RawItemStack> items() {
                return List.of(item);
            }
        };

        ContainerSnapshot fixed = ContainerSnapshot.of(Set.of(ownerId), view);
        assertEquals("container", fixed.getSnapshotName());
        RawContainer raw = fixed.data();
        Object[] payload = raw.toPayload();
        assertEquals(-4, payload[1]);
        assertEquals(70, payload[2]);
        assertEquals(128, payload[3]);
        assertEquals("", payload[4]); // null containerName normalized to "" by RawObject.toPayload()
        assertEquals(false, payload[6]);

        ContainerSnapshot portable = ContainerSnapshot.ofPortable(Set.of(ownerId), portableId, view);
        assertEquals("portable:" + portableId, portable.key());

        LoadStationSnapshot loadStation = LoadStationSnapshot.ofDeleted(Set.of(ownerId), view);
        assertEquals("load-station", loadStation.getSnapshotName());
        assertTrue(loadStation.key().startsWith("load-station:"));
        assertEquals(true, loadStation.data().deleted());
    }
}
