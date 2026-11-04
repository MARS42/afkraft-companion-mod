package com.mars4two.ikang.core.events;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.IsServerAuthenticatedResponse;
import com.mars4two.ikang.core.network.payloads.server.AuthorizeServerResponseServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.IsServerAuthenticatedRequest;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.storage.AFKContainerConfigData;
import com.mars4two.ikang.core.storage.ContainerTrackingMode;
import com.mars4two.ikang.core.util.BlockEntityWrapper;
import com.mars4two.ikang.core.util.PortableContainerIdentityHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        if(isDedicated && AuthService.isAuthenticated()) {
            log.info("Dedicated server detected and authenticated, sending authorize server request to player {}", playerId);

            // TODO Implement "Remember server" feature
//            if(AuthService.isPlayerAuthorized(playerId)){
//                log.info("Player {} already authorized the server", playerId);
//                player.sendSystemMessage(Component.literal("Bienvenido de nuevo, ya has autorizado este servidor para AFKraft"));
//                return;
//            }
            sendAuthorizeServerRequestToPlayer(player);
        }
    }

    public static void onServerAuthorizedByPlayer(MinecraftServer server, ServerPlayer player, AuthorizeServerResponseServerboundPayload payload) {
        var profileId = player.getGameProfile().id();
        log.info("Server has been authorized by player {}", player.getGameProfile().id());
        AuthService.authorizePlayer(profileId);
    }

    public static void onIsServerAuthenticatedRequest(ServerPlayer player, IsServerAuthenticatedRequest payload) {
        ServerPlayNetworking.send(player, new IsServerAuthenticatedResponse(AuthService.isAuthenticated(), Optional.ofNullable(AuthService.getCredentialsOwner()).orElse("")));
    }

    public static void onGetCredentialsRequest(ServerPlayer admin, GetCredentialsRequestServerboundPayload payload) {
        log.info("Server received credentials request payload for user {}: {}", admin.getGameProfile().id(), payload);
        AuthService.authenticate(payload, admin, () -> {
            admin.sendSystemMessage(Component.literal("Credenciales AFKraft generadas correctamente"));
            var level = admin.level().getServer();
            var isDedicated = level.isDedicatedServer();
            if(!isDedicated) {
                log.info("Server is not dedicated, no need to send authorize server request to players");
                return;
            }

            level.getPlayerList().getPlayers().forEach(ServerEvents::sendAuthorizeServerRequestToPlayer);
        }, () -> admin.sendSystemMessage(Component.literal("Error al generar credenciales AFKraft")));
    }

    private static void sendAuthorizeServerRequestToPlayer(ServerPlayer player)
    {
        var profileId = player.getGameProfile().id();
        AuthService.checkIfIsConnected(profileId, isConnected -> {
            if(Boolean.FALSE.equals(isConnected)) {
                log.info("Player {} is not connected, skipping authorize server request", profileId);
                Component clickableWord = Component.literal("[vincula]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withBold(true)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand("/afkraft connect"))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Haz clic aquí para iniciar la vinculación")))
                        );

                Component fullMessage = Component.literal("El servidor ha activado AFKraft, pero tú no has enlazado tu cuenta con AFKraft. Por favor, ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(clickableWord)
                        .append(Component.literal(" tu cuenta para poder autorizar el servidor.").withStyle(ChatFormatting.YELLOW));

                player.sendSystemMessage(fullMessage);
                return;
            }

            log.info("Sending authorize server request to player {}", profileId);
            player.level().getServer().execute(() -> ServerPlayNetworking.send(player, new AuthorizeServerRequestClientboundPayload(AuthService.getCurrentServerId().toString())));
        });
    }

    public static InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos blockPos, Player player, BlockHitResult hitResult) {
        if(level.isClientSide()){
            return null;
        }

        var serverPlayer = (ServerPlayer) player;
        var playerId = serverPlayer.getGameProfile().id();

        lastUsedItemsByPlayers.put(playerId, resolveAfkBlockPositions(blockPos, state));

        log.debug("Current used item for player {} at pos {}", playerId, blockPos);

        var containerConfig = AFKContainerConfigData.getConfig(level.getServer());
        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        var effectiveData = containerConfig.resolveEffectiveData(serverPlayer, blockPos, blockEntity);
        var afkraftStatus = new AFKraftStatusClientBoundPayload(true, AuthService.isAuthenticated(), effectiveData);
        ServerPlayNetworking.send(serverPlayer, afkraftStatus);

        return null;
    }

    public static void onRegisterAfkContainer(MinecraftServer server, ServerPlayer player, RegisterAFKContainerServerPayload payload) {
        var playerId = player.getGameProfile().id();
        var mode = payload.mode();

        var positions = lastUsedItemsByPlayers.get(playerId);

        if(positions == null || positions.isEmpty()){
            log.error("Last used item for player with id {} hasn't been found!", playerId);
            return;
        }

        var containerConfig = AFKContainerConfigData.getConfig(server);
        var level = player.level();
        var ingestManager = SnapshotIngestManager.getInstance();

        positions.forEach(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            BlockEntityWrapper wrapper = (blockEntity instanceof BlockEntityWrapper bew) ? bew : null;

            var previousData = containerConfig.resolveEffectiveData(player, pos, blockEntity);
            var previousMode = previousData.enabled() ? previousData.mode() : ContainerTrackingMode.OFF;

            if (previousMode != mode && previousMode.isEnabled() && wrapper != null) {
                switch (previousMode) {
                    case PORTABLE_CONTAINER -> {
                        PortableContainerIdentityHelper.getContainerId(blockEntity).ifPresent(containerId -> {
                            ingestManager.ingestPortableContainerDeleted(Set.of(playerId), containerId, wrapper);
                        });
                    }
                    case LOAD_STATION -> ingestManager.ingestLoadStationDeleted(Set.of(playerId), wrapper);
                    case CONTAINER -> ingestManager.ingestContainerDeleted(Set.of(playerId), wrapper);
                    default -> {}
                }
            }

            if (mode == ContainerTrackingMode.PORTABLE_CONTAINER && PortableContainerIdentityHelper.isPortableContainer(blockEntity)) {
                containerConfig.setPositionalMode(player, pos, ContainerTrackingMode.OFF);
                UUID containerId = PortableContainerIdentityHelper.getOrCreateContainerId(blockEntity);
                containerConfig.setPortableContainer(player, containerId, true);
                blockEntity.setChanged();
                return;
            }

            if (blockEntity != null) {
                PortableContainerIdentityHelper.getContainerId(blockEntity).ifPresent(containerId -> {
                    containerConfig.setPortableContainer(player, containerId, false);
                    if (!containerConfig.isPortableContainerEnabled(containerId)) {
                        PortableContainerIdentityHelper.removeContainerId(blockEntity);
                    }
                });
            }

            containerConfig.setPositionalMode(player, pos, mode);
            if (mode != null && mode.isEnabled() && blockEntity != null) {
                blockEntity.setChanged();
            }
        });
        log.info("Item with class {}, has been updated in saved data with mode {}", payload.containerType(), mode);
    }

    public static void onPlayerBlockBreak(ServerPlayer player, BlockPos blockPos, BlockState blockState) {
        var level = player.level();
        if(level.isClientSide()) {
            return;
        }

        var containerConfig = AFKContainerConfigData.getConfig(level.getServer());
        var existing = containerConfig.getContainerData(player, blockPos);
        if (existing.isPresent() && existing.get().mode() == ContainerTrackingMode.LOAD_STATION) {
            return;
        }
        containerConfig.deleteFixedContainersAt(level.dimension().identifier(), blockPos);
    }

    public static List<BlockPos> resolveAfkBlockPositions(BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() instanceof ChestBlock
                && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos connectedBlockPos = ChestBlock.getConnectedBlockPos(blockPos, blockState);
            return List.of(blockPos, connectedBlockPos);
        }

        return List.of(blockPos);
    }

    public static void onServerStarted(MinecraftServer server) {
        SnapshotIngestManager.getInstance().start(server);
    }

    public static void onServerClosed(MinecraftServer server) {
        SnapshotIngestManager.getInstance().close();
        AuthService.clearModCredentials();
    }
}
