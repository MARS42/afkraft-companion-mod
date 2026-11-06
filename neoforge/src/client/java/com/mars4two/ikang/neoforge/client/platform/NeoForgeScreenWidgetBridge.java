package com.mars4two.ikang.neoforge.client.platform;

import com.mars4two.ikang.client.platform.services.ScreenWidgetBridge;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeScreenWidgetBridge implements ScreenWidgetBridge {

    @Override
    public void onAfterScreenInit(AfterScreenInitListener listener) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            listener.onAfterInit(event.getScreen(), event::addListener);
        });
    }
}
