package com.mars4two.ikang.client;

import com.mars4two.ikang.client.command.AFKraftClientCommand;
import com.mars4two.ikang.client.core.worker.AFKraftWorker;
import com.mars4two.ikang.client.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AFKraftCompanionClient extends AFKraftWorker implements ClientModInitializer {

	private static final Logger log = LoggerFactory.getLogger("AFKraft-Client");
	private static final List<Class<? extends AbstractContainerMenu>> AFKeablesMenues = List.of(BlastFurnaceMenu.class, ChestMenu.class, CrafterMenu.class, DispenserMenu.class, FurnaceMenu.class, HopperMenu.class, InventoryMenu.class,ShulkerBoxMenu.class, SmokerMenu.class);

	@Override
	public void onInitializeClient() {
		// Registrar comandos client-side
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			AFKraftClientCommand.register(dispatcher);
		});

		ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClosed);



		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			log.info("Screen initialized: {}", screen.getClass().getSimpleName());
			if (screen instanceof AbstractContainerScreen<?> handledScreen) {

				if(AFKeablesMenues.contains(handledScreen.getMenu().getClass())) {
					AbstractContainerMenu menu = handledScreen.getMenu();

					var accessor = (AbstractContainerScreenAccessor) handledScreen;
					int guiLeft = accessor.leftPos();
					int guiTop = accessor.topPos();
					int bgWidth = handledScreen.width;

					var button = Button.builder(Component.literal("AFK"), b -> {

						log.info("Botón pulsado en {}", screen.getClass().getSimpleName());
						log.info("Menu: {}", menu.containerId);
					})
							.bounds(bgWidth - guiLeft + 4, guiTop + 4, 32, 16)
							.build();
					Screens.getWidgets(screen).add(button);
				}
			}
		});
	}
}