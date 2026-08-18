package com.mars4two.ikang;

import com.mars4two.ikang.command.AFKraftConnectCommand;
import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.events.ServerEvents;
import com.mars4two.ikang.network.PayloadRegister;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsStatusServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AFKraftCompanion implements ModInitializer {
	public static final String MOD_ID = "afkraft-companion";
	public static final Logger log = LoggerFactory.getLogger(MOD_ID);



	@Override
	public void onInitialize() {
		log.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register(AFKraftConnectCommand::register);

        PayloadRegister.registerClientboundPayloads();
        PayloadRegister.registerServerboundPayloads();

		ServerPlayerEvents.JOIN.register(ServerEvents::onJoinServerPlayer);
		BlockEvents.USE_WITHOUT_ITEM.register(ServerEvents::onUseWithoutItem);

		ServerPlayNetworking.registerGlobalReceiver(GetCredentialsStatusServerboundPayload.TYPE, (payload, context) -> {
			log.info("Server received credentials status payload for user {}: {}", context.player().getGameProfile().id(), payload);
			// TODO: Evitar este usuario en eventos
		});

		ServerPlayNetworking.registerGlobalReceiver(GetCredentialsRequestServerboundPayload.TYPE, (payload, context) -> {
			log.info("Server received credentials request payload for user {}: {}", context.player().getGameProfile().id(), payload);
			// TODO: Obtener credenciales de este usuario con el OTP recibido
			AuthService.authenticate(payload);
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
