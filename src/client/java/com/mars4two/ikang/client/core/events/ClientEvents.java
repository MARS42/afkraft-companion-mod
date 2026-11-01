package com.mars4two.ikang.client.core.events;

import com.mars4two.ikang.client.core.auth.ClientAuthService;
import com.mars4two.ikang.client.core.auth.ClientAuthorizeServerService;
import com.mars4two.ikang.client.mixin.AbstractContainerScreenAccessor;
import com.mars4two.ikang.core.network.payloads.client.AFKraftStatusClientBoundPayload;
import com.mars4two.ikang.core.network.payloads.client.AuthorizeServerRequestClientboundPayload;
import com.mars4two.ikang.core.network.payloads.client.GetCredentialsStatusClientboundPayload;
import com.mars4two.ikang.core.network.payloads.server.AuthorizeServerResponseServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.GetCredentialsRequestServerboundPayload;
import com.mars4two.ikang.core.network.payloads.server.RegisterAFKContainerServerPayload;
import com.mars4two.ikang.core.storage.ContainerTrackingMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ClientEvents {

    private static final Logger log = LoggerFactory.getLogger(ClientEvents.class);

    private static final AtomicReference<ContainerTrackingMode> afkContainerMode = new AtomicReference<>(ContainerTrackingMode.OFF);
    private static final AtomicBoolean afkraftEnabled = new AtomicBoolean(false);

    private static final List<Class<? extends AbstractContainerMenu>> AFKeablesMenues = List.of(
            BlastFurnaceMenu.class,
            ChestMenu.class,
            CrafterMenu.class,
            DispenserMenu.class,
            FurnaceMenu.class,
            HopperMenu.class,
            ShulkerBoxMenu.class,
            SmokerMenu.class
    );

    private static final List<Class<? extends AbstractContainerMenu>> PORTABLE_CONTAINER_MENUS = List.of(
            ShulkerBoxMenu.class
    );

    private ClientEvents() {
        // Utility class, no instances allowed
    }

    public static void onClientHasConnected(UUID profileId, String otp) {
        log.info("Client has connected, sending credentials request");
        ClientAuthService.authenticate(
                (serverId, joinOtp) -> Minecraft.getInstance().execute(
                        () -> ClientPlayNetworking.send(new GetCredentialsRequestServerboundPayload(profileId.toString(), serverId, joinOtp))
                ),
                (e) -> log.error("Error while authenticating", e),
                () -> log.info("Client is not connected")
        );
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
    }

    public static void onAuthorizeServerRequest(AuthorizeServerRequestClientboundPayload payload, Player player) {
        log.info("Server wants to authorize server join, payload");
        var authorizeService = new ClientAuthorizeServerService(UUID.fromString(payload.currentServerId()),player,
                () -> ClientPlayNetworking.send(new AuthorizeServerResponseServerboundPayload()));
        authorizeService.showAuthorizationScreen();
    }

    public static void onGetAfkraftStatus(AFKraftStatusClientBoundPayload afkraftStatus) {
        log.info("Received AFKraftStatus payload {}", afkraftStatus);

        ContainerTrackingMode mode = (afkraftStatus.containerData() != null && afkraftStatus.containerData().enabled())
                ? afkraftStatus.containerData().mode()
                : ContainerTrackingMode.OFF;

        afkContainerMode.set(mode);
        afkraftEnabled.set(afkraftStatus.enabled() && afkraftStatus.authenticated());

        log.info("AFKraft enabled: {}", afkraftEnabled.get());
        log.info("AFK container mode: {}", afkContainerMode.get());
    }

    public static void onAfterScreenInit(Screen screen) {
        log.info("Screen initialized: {}", screen.getClass().getSimpleName());
        if (screen instanceof AbstractContainerScreen<?> handledScreen) {
            Class<? extends AbstractContainerMenu> menuClass = handledScreen.getMenu().getClass();

            if (AFKeablesMenues.contains(menuClass)) {
                AbstractContainerMenu menu = handledScreen.getMenu();

                var accessor = (AbstractContainerScreenAccessor) handledScreen;
                int guiLeft = accessor.leftPos();
                int guiTop = accessor.topPos();
                int bgWidth = handledScreen.width;
                int buttonX = bgWidth - guiLeft + 4;
                int buttonY = guiTop + 4;

                if (PORTABLE_CONTAINER_MENUS.contains(menuClass)) {
                    ContainerTrackingMode initialMode = afkContainerMode.get();
                    if (initialMode == ContainerTrackingMode.CONTAINER) {
                        initialMode = ContainerTrackingMode.PORTABLE_CONTAINER;
                    }

                    var portableButton = CycleButton.<ContainerTrackingMode>builder(
                                    ClientEvents::formatPortableModeLabel,
                                    initialMode
                            )
                            .withValues(
                                    ContainerTrackingMode.OFF,
                                    ContainerTrackingMode.PORTABLE_CONTAINER,
                                    ContainerTrackingMode.LOAD_STATION
                            )
                            .create(buttonX, buttonY, 84, 16, Component.literal("AFK"),
                                    (b, mode) -> onClickButton(screen, menu, mode));
                    portableButton.active = afkraftEnabled.get();
                    Screens.getWidgets(screen).add(portableButton);
                } else {
                    var containerButton = CycleButton.onOffBuilder(afkContainerMode.get().isEnabled())
                            .create(buttonX, buttonY, 48, 16, Component.literal("AFK"),
                                    (b, enabled) -> onClickButton(screen, menu, enabled ? ContainerTrackingMode.CONTAINER : ContainerTrackingMode.OFF));
                    containerButton.active = afkraftEnabled.get();
                    Screens.getWidgets(screen).add(containerButton);
                }
            }
        }
    }

    private static Component formatPortableModeLabel(ContainerTrackingMode mode) {
        return switch (mode) {
            case PORTABLE_CONTAINER -> Component.literal("Shulker");
            case LOAD_STATION -> Component.literal("Posición");
            default -> Component.literal("Off");
        };
    }

    private static void onClickButton(Screen screen, AbstractContainerMenu menu, ContainerTrackingMode mode) {
        var screenClass = screen.getClass().getCanonicalName();
        log.info("Botón pulsado en {} con modo {}", screen.getClass().getSimpleName(), mode);
        log.info("Menu: {}", menu.containerId);

        var registerAFKContainerPayload = new RegisterAFKContainerServerPayload(screenClass, mode);
        ClientPlayNetworking.send(registerAFKContainerPayload);
    }
}
