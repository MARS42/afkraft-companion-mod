package com.mars4two.ikang.platform.services;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface NetworkBridge {

    @FunctionalInterface
    interface ServerPayloadHandler<T extends CustomPacketPayload> {
        void handle(T payload, MinecraftServer server, ServerPlayer player);
    }

    @FunctionalInterface
    interface ClientPayloadHandler<T extends CustomPacketPayload> {
        void handle(T payload, Player player);
    }

    <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    );

    <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ServerPayloadHandler<T> handler
    );

    <T extends CustomPacketPayload> void registerClientReceiver(
            CustomPacketPayload.Type<T> type,
            ClientPayloadHandler<T> handler
    );

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToServer(CustomPacketPayload payload);
}
