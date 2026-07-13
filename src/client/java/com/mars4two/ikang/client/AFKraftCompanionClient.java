package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class AFKraftCompanionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Registrar comandos client-side
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			AFKraftClientCommand.register(dispatcher);
		});
	}
}