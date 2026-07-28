package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientCommand;
import com.mars4two.ikang.client.core.worker.AFKraftWorker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AFKraftCompanionClient extends AFKraftWorker implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Registrar comandos client-side
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			AFKraftClientCommand.register(dispatcher);
		});

		ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClosed);
	}
}