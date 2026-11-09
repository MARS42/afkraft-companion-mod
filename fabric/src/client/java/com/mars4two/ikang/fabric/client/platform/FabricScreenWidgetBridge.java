package com.mars4two.ikang.fabric.client.platform;

import com.mars4two.ikang.client.platform.services.ScreenWidgetBridge;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

public final class FabricScreenWidgetBridge implements ScreenWidgetBridge {

    @Override
    public void onAfterScreenInit(AfterScreenInitListener listener) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            //? if >=26.1 {
            listener.onAfterInit(screen, widget -> Screens.getWidgets(screen).add(widget));
            //?} else {
            /*listener.onAfterInit(screen, widget -> Screens.getButtons(screen).add(widget));
            *///?}
        });
    }
}
