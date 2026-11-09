package com.mars4two.ikang.fabric.platform;

import com.mars4two.ikang.platform.services.NetworkBridge;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class FabricNetworkBridge implements NetworkBridge {

    public interface ClientHook {
        <T extends CustomPacketPayload> void registerReceiver(
                CustomPacketPayload.Type<T> type,
                ClientPayloadHandler<T> handler
        );

        void sendToServer(CustomPacketPayload payload);
    }

    private static volatile ClientHook clientHook;

    public static void installClientHook(ClientHook hook) {
        clientHook = Objects.requireNonNull(hook, "clientHook");
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        //? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        //?} else {
        /*PayloadTypeRegistry.playS2C().register(type, codec);
        *///?}
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ServerPayloadHandler<T> handler
    ) {
        //? if >=26.1 {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(type, codec);
        *///?}
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            if (server.isSameThread()) {
                handler.handle(payload, server, player);
            } else {
                server.execute(() -> handler.handle(payload, server, player));
            }
        });
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientReceiver(
            CustomPacketPayload.Type<T> type,
            ClientPayloadHandler<T> handler
    ) {
        ClientHook hook = clientHook;
        if (hook == null) {
            throw new IllegalStateException("Fabric client network hook is not installed");
        }
        hook.registerReceiver(type, handler);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientHook hook = clientHook;
        if (hook == null) {
            throw new IllegalStateException("Cannot send serverbound packet without client network hook");
        }
        hook.sendToServer(payload);
    }
}
