package com.mars4two.ikang.core.events;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerEvents {

    public static final Logger log = LoggerFactory.getLogger(ServerEvents.class);

    private static final Map<UUID, List<BlockPos>> lastUsedItemsByPlayers = new ConcurrentHashMap<>();

    private ServerEvents() {
        // Utility class, no instances allowed
    }

    public static void onJoinServerPlayer(ServerPlayer player) {
        var playerId = player.getGameProfile().id();
        var level = player.level().getServer();

        var isDedicated = level.isDedicatedServer();
        var isSinglePlayer = level.isSingleplayer();
        var isSinglePlayerOwner = level.isSingleplayerOwner(new NameAndId(player.getGameProfile()));

        log.info("Player {} joined the server, (dedicated={}, singleplayer={}, owner={})",
                playerId, level.isDedicatedServer(), level.isSingleplayer(), isSinglePlayerOwner);

        if(isSinglePlayer && isSinglePlayerOwner) {
            log.info("Singleplayer owner detected, sending get credentials request to player {}", playerId);
            ServerPlayNetworking.send(player,
                    new GetCredentialsStatusClientboundPayload(playerId.toString(), true));
            return;
        }

        if(isDedicated) {
            log.info("Dedicated server detected, sending authorize server request to player {}", playerId);

            // Todo: Check if admin already logged in and server has credentials

            ServerPlayNetworking.send(player, new AuthorizeServerRequestClientboundPayload());
        }
    }

    public static InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos blockPos, Player player, BlockHitResult hitResult) {
        if(level.isClientSide()){
            return null;
        }

        var playerId = player.getGameProfile().id();

        lastUsedItemsByPlayers.put(playerId, resolveAfkBlockPositions(blockPos, state));

        log.debug("Current used item for player {} at pos {}", playerId, blockPos);

        var containerConfig = AFKContainerConfigData.getConfig(level.getServer());

        var payload = containerConfig.getContainerData((ServerPlayer) player, blockPos);
        if(payload.isPresent()){
            log.info("Container data found, sending configuration to player {} for block pos {}", playerId, blockPos);
            var afkraftStatus = new AFKraftStatusClientBoundPayload(true, AuthService.isAuthenticated(), payload.get());
            ServerPlayNetworking.send((ServerPlayer) player, afkraftStatus);
        } else {
            // Send empty configuration for not configured blocks.
            var afkraftStatus = new AFKraftStatusClientBoundPayload(true, AuthService.isAuthenticated(), new AFKContainerConfigData.ContainerData(false));
            ServerPlayNetworking.send((ServerPlayer) player, afkraftStatus);
        }

        return null;
    }

    public static void onRegisterAfkContainer(MinecraftServer server, ServerPlayer player, RegisterAFKContainerServerPayload payload) {
        var playerId = player.getGameProfile().id();
        var containerEnabled = payload.enabled();

        var positions = lastUsedItemsByPlayers.get(playerId);

        if(positions.isEmpty()){
            log.error("Last used item for player with id {} hasn't been found!", playerId);
            return;
        }

        var containerConfig = AFKContainerConfigData.getConfig(server);

        positions.forEach(pos -> containerConfig.set(player, pos, containerEnabled));
        log.info("Item with class {}, has been updated in saved data with value {}", payload.containerType(), containerEnabled);
    }

    public static void onPlayerBlockBreak(ServerPlayer player, BlockPos blockPos, BlockState blockState) {
        var level = player.level();
        if(level.isClientSide()) {
            return;
        }

        var containerConfig = AFKContainerConfigData.getConfig(level.getServer());
        containerConfig.deleteContainerData(player, blockPos);
    }

    public static List<BlockPos> resolveAfkBlockPositions(BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() instanceof ChestBlock
                && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos connectedBlockPos = ChestBlock.getConnectedBlockPos(blockPos, blockState);
            return List.of(blockPos, connectedBlockPos);
        }

        return List.of(blockPos);
    }
}
