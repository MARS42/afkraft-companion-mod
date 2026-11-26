package com.mars4two.ikang;

import com.mars4two.ikang.core.config.HttpConfig;
import com.mars4two.ikang.core.config.ModClientTelemetry;
import com.mars4two.ikang.core.events.ServerEvents;
import com.mars4two.ikang.network.PayloadRegister;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mars4two.ikang.platform.services.Services;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AFKraftCompanion {

	public static final String MOD_ID = "afkraft_companion";
	public static final String LEGACY_MOD_ID = "afkraft-companion";
	public static final Logger log = LoggerFactory.getLogger(MOD_ID);

	private AFKraftCompanion() {
	}

	public static void init() {
		log.info("Initializing AFKraft Companion common module");

		ModClientTelemetry telemetry = ModClientTelemetry.of(
				WorldAndEntityAdapter.minecraftVersionId(Services.PLATFORM_INFO.getMinecraftVersion()),
				Services.PLATFORM_INFO.getModVersion(),
				Services.PLATFORM_INFO.getLoaderName()
		);
		HttpConfig.configureClientTelemetry(telemetry);
		log.debug(
				"Configured client telemetry headers (gameVersion={}, modVersion={}, modLoader={})",
				telemetry.gameVersion(),
				telemetry.modVersion(),
				telemetry.modLoader()
		);

		PayloadRegister.registerPayloads();

		Services.SERVER_LIFECYCLE.onServerStarted(ServerEvents::onServerStarted);
		Services.SERVER_LIFECYCLE.onServerStopped(ServerEvents::onServerClosed);
		Services.SERVER_LIFECYCLE.onPlayerJoin(ServerEvents::onJoinServerPlayer);

		Services.BLOCK_INTERACTION.onBlockUse(ServerEvents::onBlockUse);
		Services.BLOCK_INTERACTION.onAfterBlockBreak(ServerEvents::onPlayerBlockBreak);
	}

	public static Identifier id(String path) {
		return WorldAndEntityAdapter.modId(path);
	}
}
