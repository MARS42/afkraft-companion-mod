package com.mars4two.ikang.client.platform.services;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public interface ScreenWidgetBridge {

    @FunctionalInterface
    interface AfterScreenInitListener {
        void onAfterInit(Screen screen, Consumer<AbstractWidget> addWidget);
    }

    void onAfterScreenInit(AfterScreenInitListener listener);
}
