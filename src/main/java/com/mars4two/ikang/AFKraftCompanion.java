package com.mars4two.ikang;

import com.mars4two.ikang.core.events.ServerEvents;
import com.mars4two.ikang.core.network.payloads.server.AuthorizeServerResponseServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.IsServerAuthenticatedRequest;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.network.PayloadRegister;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AFKraftCompanion implements ModInitializer {

	public static final String MOD_ID = "afkraft_companion";
	public static final String LEGACY_MOD_ID = "afkraft-companion";
	public static final Logger log = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		log.info("Hello Fabric world!");

        PayloadRegister.registerClientboundPayloads();
        PayloadRegister.registerServerboundPayloads();

		ServerLifecycleEvents.SERVER_STARTED.register(ServerEvents::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(ServerEvents::onServerClosed);

		ServerPlayerEvents.JOIN.register(ServerEvents::onJoinServerPlayer);
		BlockEvents.USE_WITHOUT_ITEM.register(ServerEvents::onUseWithoutItem);

		ServerPlayNetworking.registerGlobalReceiver(AuthorizeServerResponseServerboundPayload.TYPE, (payload, context) -> {
			ServerEvents.onServerAuthorizedByPlayer(context.server(), context.player(), payload);
		});

		ServerPlayNetworking.registerGlobalReceiver(IsServerAuthenticatedRequest.TYPE, (payload, context) -> {
			ServerEvents.onIsServerAuthenticatedRequest(context.player(), payload);
		});

		ServerPlayNetworking.registerGlobalReceiver(GetCredentialsRequestServerboundPayload.TYPE, (payload, context) -> {
			ServerEvents.onGetCredentialsRequest(context.player(), payload);
		});

		ServerPlayNetworking.registerGlobalReceiver(RegisterAFKContainerServerPayload.PAYLOAD_TYPE,
				(payload, context)
						-> ServerEvents.onRegisterAfkContainer(context.server(), context.player(), payload)
		);

		PlayerBlockBreakEvents.AFTER.register(
				(level, player, blockPos, state, blockEntity) ->
						ServerEvents.onPlayerBlockBreak((ServerPlayer) player, blockPos, state)
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
