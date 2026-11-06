package com.mars4two.ikang.neoforge;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.client.AFKraftCompanionClient;
import com.mars4two.ikang.neoforge.platform.NeoForgeBlockInteractionHooks;
import com.mars4two.ikang.neoforge.platform.NeoForgeNetworkBridge;
import com.mars4two.ikang.platform.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeRuntimeRegressionTest {

    private record SyntheticOrphanPayload() implements CustomPacketPayload {
        static final Type<SyntheticOrphanPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(AFKraftCompanion.MOD_ID, "test_orphan_clientbound"));
        static final StreamCodec<RegistryFriendlyByteBuf, SyntheticOrphanPayload> STREAM_CODEC =
                StreamCodec.unit(new SyntheticOrphanPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyClientboundPayloadRegisteredByModHasClientHandlerInNeoForge() throws Exception {
        NeoForgeNetworkBridge.installClientHook(payload -> {
        });
        AFKraftCompanion.init();
        AFKraftCompanionClient.init();
        Services.NETWORK.registerClientbound(SyntheticOrphanPayload.TYPE, SyntheticOrphanPayload.STREAM_CODEC);

        NeoForgeNetworkBridge.onRegisterPayloadHandlers(new RegisterPayloadHandlersEvent());

        Field registrationsField = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
        registrationsField.setAccessible(true);
        var payloadRegistrations = (Map<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>>) registrationsField.get(null);

        Field clientHandlersField = NetworkRegistry.class.getDeclaredField("CLIENTBOUND_HANDLERS");
        clientHandlersField.setAccessible(true);
        var clientboundHandlers = (Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>>) clientHandlersField.get(null);

        Map<Identifier, PayloadRegistration<?>> playRegistrations = payloadRegistrations.get(ConnectionProtocol.PLAY);
        Map<Identifier, IPayloadHandler<?>> playClientHandlers = clientboundHandlers.get(ConnectionProtocol.PLAY);

        List<Identifier> missingClientHandlers = playRegistrations.values().stream()
                .filter(reg -> reg.matchesFlow(PacketFlow.CLIENTBOUND))
                .map(PayloadRegistration::id)
                .filter(id -> !playClientHandlers.containsKey(id))
                .toList();

        assertTrue(
                missingClientHandlers.isEmpty(),
                () -> "ClientNetworkRegistry.setup() would fail with missing client-side handlers: " + missingClientHandlers
        );
        assertFalse(
                playRegistrations.containsKey(Identifier.fromNamespaceAndPath(AFKraftCompanion.MOD_ID, "afk_container_data_payload")),
                "Nested ContainerData value record must not be registered as a top-level custom payload"
        );
    }

    @Test
    void afterBlockBreakDefersCallbackUntilAfterBlockRemovalAndSkipsUnbrokenBlocks() {
        List<BlockPos> brokenPositions = new ArrayList<>();
        new NeoForgeBlockInteractionHooks().onAfterBlockBreak(
                (player, level, pos, state) -> brokenPositions.add(pos)
        );

        // Non-server BreakBlockEvent on NeoForge.EVENT_BUS must safely no-op.
        NeoForge.EVENT_BUS.post(new BreakBlockEvent(null, new BlockPos(1, 2, 3), null, null));
        assertTrue(brokenPositions.isEmpty());

        Deque<Runnable> scheduledTasks = new ArrayDeque<>();
        AtomicBoolean blockRemoved = new AtomicBoolean(false);
        List<BlockPos> checkedPositions = new ArrayList<>();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(10, 64, -20);

        // 1. Pre-break phase: BreakBlockEvent fires BEFORE removeBlock and BlockEntity.preRemoveSideEffects.
        NeoForgeBlockInteractionHooks.handleBreakEvent(
                scheduledTasks::addLast,
                pos -> {
                    checkedPositions.add(pos);
                    return blockRemoved.get();
                },
                mutablePos,
                brokenPositions::add
        );
        assertEquals(
                0,
                brokenPositions.size(),
                "onAfterBlockBreak must NOT fire synchronously during BreakBlockEvent before BlockEntity.preRemoveSideEffects"
        );
        assertEquals(1, scheduledTasks.size(), "Expected one deferred post-break task to be scheduled");

        // Mutate caller's MutableBlockPos to verify defensive immutable copy, and keep block unbroken (removeBlock == false).
        mutablePos.set(999, 999, 999);
        scheduledTasks.removeFirst().run();
        assertEquals(List.of(new BlockPos(10, 64, -20)), checkedPositions);
        assertTrue(brokenPositions.isEmpty(), "onAfterBlockBreak must not fire when block was not actually removed");

        // 2. Successful break phase: block was removed before deferred task runs.
        BlockPos.MutableBlockPos secondPos = new BlockPos.MutableBlockPos(-5, 70, 12);
        NeoForgeBlockInteractionHooks.handleBreakEvent(
                scheduledTasks::addLast,
                pos -> {
                    checkedPositions.add(pos);
                    return blockRemoved.get();
                },
                secondPos,
                brokenPositions::add
        );
        assertTrue(brokenPositions.isEmpty());
        assertEquals(1, scheduledTasks.size());

        secondPos.set(111, 222, 333);
        blockRemoved.set(true);
        scheduledTasks.removeFirst().run();

        assertEquals(List.of(new BlockPos(-5, 70, 12)), brokenPositions);
    }
}
