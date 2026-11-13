package com.mars4two.ikang.platform.mc;

import com.mars4two.ikang.AFKraftCompanion;
import com.mars4two.ikang.core.model.BlockCoord;
import com.mars4two.ikang.core.model.DimensionKey;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.9 {
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelData;
//?} else {
/*import net.minecraft.world.entity.EquipmentSlot;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.UUID;

/**
 * Isolates world, dimension, identifier, registry, profile, chat action, and entity helper
 * differences across Minecraft 1.21.1 and 26.2.
 */
public final class WorldAndEntityAdapter {

    private WorldAndEntityAdapter() {
    }

    public static Identifier modId(String path) {
        return Identifier.fromNamespaceAndPath(AFKraftCompanion.MOD_ID, path);
    }

    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier defaultId(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public static Identifier parseId(String raw) {
        return Identifier.parse(raw);
    }

    public static ServerLevel serverLevel(ServerPlayer player) {
        //? if >=1.21.6 {
        return player.level();
        //?} else {
        /*return player.serverLevel();
        *///?}
    }

    public static UUID profileId(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.id();
        //?} else {
        /*return profile.getId();
        *///?}
    }

    public static String profileName(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.name();
        //?} else {
        /*return profile.getName();
        *///?}
    }

    public static UUID playerId(Player player) {
        return profileId(player.getGameProfile());
    }

    public static String playerName(Player player) {
        return profileName(player.getGameProfile());
    }

    public static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        //? if >=1.21.9 {
        return server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
        //?} else {
        /*return server.isSingleplayerOwner(player.getGameProfile());
        *///?}
    }

    public static Identifier dimensionId(Level level) {
        //? if >=1.21.11 {
        return level.dimension().identifier();
        //?} else {
        /*return level.dimension().location();
        *///?}
    }

    public static DimensionKey dimensionKey(Level level) {
        return new DimensionKey(dimensionId(level).toString());
    }

    public static String dimensionString(Level level) {
        return dimensionId(level).toString();
    }

    public static Optional<BlockCoord> resolveDimensionSpawn(ServerLevel level) {
        //? if >=1.21.9 {
        LevelData.RespawnData respawnData = level.getRespawnData();
        if (respawnData != null && respawnData.dimension().equals(level.dimension())) {
            BlockPos pos = respawnData.pos();
            return Optional.of(new BlockCoord(pos.getX(), pos.getY(), pos.getZ()));
        }
        return Optional.empty();
        //?} else {
        /*if (level.dimension().equals(Level.OVERWORLD)) {
            BlockPos pos = level.getSharedSpawnPos();
            return Optional.of(new BlockCoord(pos.getX(), pos.getY(), pos.getZ()));
        }
        return Optional.empty();
        *///?}
    }

    public static int minY(Level level) {
        //? if >=1.21.2 {
        return level.getMinY();
        //?} else {
        /*return level.getMinBuildHeight();
        *///?}
    }

    public static int maxY(Level level) {
        //? if >=1.21.2 {
        return level.getMaxY();
        //?} else {
        /*return level.getMaxBuildHeight();
        *///?}
    }

    public static int spawnableChunkCount(ServerChunkCache chunkCache, NaturalSpawner.SpawnState lastSpawnState) {
        return lastSpawnState != null
                ? lastSpawnState.getSpawnableChunkCount()
                : chunkCache.chunkMap.getDistanceManager().getNaturalSpawnChunkCount();
    }

    public static float tickRate(Level level) {
        return level.tickRateManager().tickrate();
    }

    public static int handEquipmentSlotIndex(InteractionHand hand) {
        //? if >=1.21.9 {
        return hand.asEquipmentSlot().getId();
        //?} elif >=1.21.2 {
        /*return (hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).getId();
        *///?} else {
        /*return (hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).getFilterFlag();
        *///?}
    }

    public static Component buildClickableCommandWord(
            String text,
            ChatFormatting color,
            String command,
            String hoverTooltip
    ) {
        return buildClickableCommandWord(
                Component.literal(text),
                color,
                command,
                Component.literal(hoverTooltip)
        );
    }

    public static Component buildClickableCommandWord(
            Component text,
            ChatFormatting color,
            String command,
            Component hoverTooltip
    ) {
        return text.copy()
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withUnderlined(true)
                        //? if >=1.21.5 {
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(hoverTooltip))
                        //?} else {
                        /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverTooltip))
                        *///?}
                );
    }

    public static Component buildClickableUrlWord(
            Component text,
            ChatFormatting color,
            String url,
            Component hoverTooltip
    ) {
        return text.copy()
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withUnderlined(true)
                        //? if >=1.21.5 {
                        .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))
                        .withHoverEvent(new HoverEvent.ShowText(hoverTooltip))
                        //?} else {
                        /*.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverTooltip))
                        *///?}
                );
    }

    public static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String entityTypeId(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
    }

    public static boolean isSuccessfulBlockPlacement(InteractionResult result) {
        //? if >=1.21.2 {
        return result == InteractionResult.SUCCESS;
        //?} else {
        /*return result != null && result.indicateItemUse();
        *///?}
    }

    public static void scheduleNextServerTask(MinecraftServer server, Runnable task) {
        //? if >=1.21.2 {
        server.schedule(new TickTask(server.getTickCount(), task));
        //?} else {
        /*server.tell(new TickTask(server.getTickCount(), task));
        *///?}
    }
}
