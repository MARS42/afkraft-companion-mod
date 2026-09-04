package com.mars4two.ikang.client.core.events;

import com.mars4two.ikang.client.core.auth.ClientAuthService;
import com.mars4two.ikang.client.core.auth.ClientAuthorizeServerService;
import com.mars4two.ikang.client.mixin.AbstractContainerScreenAccessor;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientEvents {

    private static final Logger log = LoggerFactory.getLogger(ClientEvents.class);

    private static AtomicBoolean afkContainerEnabled = new AtomicBoolean(false);
    private static AtomicBoolean afkraftEnabled = new AtomicBoolean(false);

    private static final List<Class<? extends AbstractContainerMenu>> AFKeablesMenues = List.of(BlastFurnaceMenu.class, ChestMenu.class, CrafterMenu.class, DispenserMenu.class, FurnaceMenu.class, HopperMenu.class, InventoryMenu.class, ShulkerBoxMenu.class, SmokerMenu.class);

    private ClientEvents() {
        // Utility class, no instances allowed
    }

    public static void onGetCredentialsStatus(GetCredentialsStatusClientboundPayload payload) {
        log.info("Server wants to know if client is connected, payload: {}", payload);

        if(payload.needsCredentials()) {
            log.info("Client needs credentials");
            ClientAuthService.authenticate(
                    (serverId, joinOtp) -> Minecraft.getInstance().execute(
                            () -> ClientPlayNetworking.send(new GetCredentialsRequestServerboundPayload(payload.profileId(), serverId, joinOtp))
                    ),
                    (e) -> log.error("Error while authenticating", e),
                    () -> log.info("Client is not connected")
            );
        }
//        }else {
//            log.info("Client does not need credentials");
//            AuthService.checkIfIsConnected(UUID.fromString(payload.profileId()), isConnected -> {
//                Minecraft.getInstance().execute(
//                        () -> ClientPlayNetworking.send(new GetCredentialsStatusServerboundPayload(isConnected))
//                );
//            });
//        }
    }

    public static void onAuthorizeServerRequest(AuthorizeServerRequestClientboundPayload payload) {
        log.info("Server wants to authorize server join, payload");
        ClientAuthorizeServerService.showAuthorizationScreen();
    }

    public static void onGetAfkraftStatus(AFKraftStatusClientBoundPayload afkraftStatus) {
        log.info("Received AFKraftStatus payload {}", afkraftStatus);

        afkContainerEnabled.set(afkraftStatus.containerData() != null && afkraftStatus.containerData().enabled());
        afkraftEnabled.set(afkraftStatus.enabled() && afkraftStatus.authenticated());

        log.info("AFKraft enabled: {}", afkraftEnabled.get());
        log.info("AFK container enabled: {}", afkContainerEnabled.get());
    }

    public static void onAfterScreenInit(Screen screen) {
        log.info("Screen initialized: {}", screen.getClass().getSimpleName());
        if (screen instanceof AbstractContainerScreen<?> handledScreen) {

            if (AFKeablesMenues.contains(handledScreen.getMenu().getClass())) {
                AbstractContainerMenu menu = handledScreen.getMenu();

                var accessor = (AbstractContainerScreenAccessor) handledScreen;
                int guiLeft = accessor.leftPos();
                int guiTop = accessor.topPos();
                int bgWidth = handledScreen.width;


                var containerButton = CycleButton.onOffBuilder(afkContainerEnabled.get())
                        .create(bgWidth - guiLeft + 4, guiTop + 4, 48, 16, Component.literal("AFK"),(b, c) -> onClickButton(screen, menu, c));
                containerButton.active = afkraftEnabled.get();
                Screens.getWidgets(screen).add(containerButton);
            }
        }
    }

    private static void onClickButton(Screen screen, AbstractContainerMenu menu, boolean enabled) {
        var screenClass = screen.getClass().getCanonicalName();
        log.info("Botón pulsado en {}", screen.getClass().getSimpleName());
        log.info("Menu: {}", menu.containerId);

        var registerAFKContainerPayload = new RegisterAFKContainerServerPayload(screenClass, enabled);
        ClientPlayNetworking.send(registerAFKContainerPayload);
    }
}
