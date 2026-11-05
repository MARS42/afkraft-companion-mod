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
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelData;

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

    public static UUID profileId(GameProfile profile) {
        return profile.id();
    }

    public static String profileName(GameProfile profile) {
        return profile.name();
    }

    public static UUID playerId(Player player) {
        return player.getGameProfile().id();
    }

    public static String playerName(Player player) {
        return player.getGameProfile().name();
    }

    public static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        return server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
    }

    public static Identifier dimensionId(Level level) {
        return level.dimension().identifier();
    }

    public static DimensionKey dimensionKey(Level level) {
        return new DimensionKey(level.dimension().identifier().toString());
    }

    public static String dimensionString(Level level) {
        return level.dimension().identifier().toString();
    }

    public static Optional<BlockCoord> resolveDimensionSpawn(ServerLevel level) {
        LevelData.RespawnData respawnData = level.getRespawnData();
        if (respawnData != null && respawnData.dimension().equals(level.dimension())) {
            BlockPos pos = respawnData.pos();
            return Optional.of(new BlockCoord(pos.getX(), pos.getY(), pos.getZ()));
        }
        return Optional.empty();
    }

    public static int minY(Level level) {
        return level.getMinY();
    }

    public static int maxY(Level level) {
        return level.getMaxY();
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
        return hand.asEquipmentSlot().getId();
    }

    public static Component buildClickableCommandWord(
            String text,
            ChatFormatting color,
            String command,
            String hoverTooltip
    ) {
        return Component.literal(text)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverTooltip)))
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
}
