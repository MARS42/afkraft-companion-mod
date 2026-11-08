package com.mars4two.ikang.client.platform.services;

import com.mars4two.ikang.platform.services.Services;

public final class ClientServices {

    public static final ClientCommandBridge COMMANDS = Services.load(ClientCommandBridge.class);
    public static final ScreenWidgetBridge SCREENS = Services.load(ScreenWidgetBridge.class);

    private ClientServices() {
    }
}
