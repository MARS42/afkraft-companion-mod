package com.mars4two.ikang.neoforge.platform;

import com.mars4two.ikang.platform.services.NetworkBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NeoForgeNetworkBridge implements NetworkBridge {

    public interface ClientHook {
        void sendToServer(CustomPacketPayload payload);
    }

    private record ClientboundRegistration<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        void registerOn(
                PayloadRegistrar registrar,
                Map<CustomPacketPayload.Type<?>, ClientPayloadHandler<?>> receivers,
                boolean clientSide
        ) {
            @SuppressWarnings("unchecked")
            ClientPayloadHandler<T> handler = (ClientPayloadHandler<T>) receivers.get(type);
            if (handler != null) {
                registrar.playToClient(type, codec, (payload, context) ->
                        handler.handle(payload, context.player())
                );
            } else if (clientSide) {
                registrar.playToClient(type, codec, (payload, context) -> {
                });
            } else {
                registrar.playToClient(type, codec);
            }
        }
    }

    private record ServerboundRegistration<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ServerPayloadHandler<T> handler
    ) {
        void registerOn(PayloadRegistrar registrar) {
            registrar.playToServer(type, codec, (payload, context) -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    MinecraftServer server = serverPlayer.level().getServer();
                    if (server != null) {
                        handler.handle(payload, server, serverPlayer);
                    }
                }
            });
        }
    }

    private static final List<ClientboundRegistration<?>> CLIENTBOUND = new CopyOnWriteArrayList<>();
    private static final List<ServerboundRegistration<?>> SERVERBOUND = new CopyOnWriteArrayList<>();
    private static final Map<CustomPacketPayload.Type<?>, ClientPayloadHandler<?>> CLIENT_RECEIVERS = new ConcurrentHashMap<>();
    private static volatile ClientHook clientHook;

    public static void installClientHook(ClientHook hook) {
        clientHook = Objects.requireNonNull(hook, "clientHook");
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        boolean clientSide = clientHook != null;
        for (ClientboundRegistration<?> entry : CLIENTBOUND) {
            entry.registerOn(registrar, CLIENT_RECEIVERS, clientSide);
        }
        for (ServerboundRegistration<?> entry : SERVERBOUND) {
            entry.registerOn(registrar);
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        CLIENTBOUND.add(new ClientboundRegistration<>(type, codec));
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ServerPayloadHandler<T> handler
    ) {
        SERVERBOUND.add(new ServerboundRegistration<>(type, codec, handler));
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientReceiver(
            CustomPacketPayload.Type<T> type,
            ClientPayloadHandler<T> handler
    ) {
        CLIENT_RECEIVERS.put(type, Objects.requireNonNull(handler, "handler"));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientHook hook = clientHook;
        if (hook == null) {
            throw new IllegalStateException("Cannot send serverbound packet without NeoForge client network hook");
        }
        hook.sendToServer(payload);
    }
}
