package com.mars4two.ikang;

import com.mars4two.ikang.command.AFKraftCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AFKraftCompanion implements ModInitializer {
	public static final String MOD_ID = "afkraft-companion";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		// Registrar comandos
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			AFKraftCommand.register(dispatcher);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
