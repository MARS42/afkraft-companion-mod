package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.screens.AuthorizeServerRequestScreen;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientAuthorizeServerService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-server-service");

    public static void showAuthorizationScreen() {
        Minecraft client = Minecraft.getInstance();
        var serverData = client.getCurrentServer();
        if(serverData == null) {
            log.info("No server selected");
            return;
        }

        var serverName = serverData.name;
        client.execute(() -> client.setScreenAndShow(new AuthorizeServerRequestScreen(
                null,
                serverName,
                authorized -> {
                    if (!Boolean.TRUE.equals(authorized)) {
                        log.info("User doesn't want authorize the server");
                        return;
                    }
                    ClientAuthService.authorizeServer(serverName);
                }
        )));
    }
}
