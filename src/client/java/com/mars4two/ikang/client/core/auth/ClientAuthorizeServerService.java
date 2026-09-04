package com.mars4two.ikang.client.core.auth;

import com.mars4two.ikang.client.core.screens.AuthorizeServerRequestScreen;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientAuthorizeServerService {

    private static final Logger log = LoggerFactory.getLogger("afkraft-auth-server-service");

    public static void showAuthorizationScreen() {
        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            client.setScreenAndShow(new AuthorizeServerRequestScreen(
                    null,
                    "XD",    // Nombre a mostrar en el placeholder
                    authorized -> {
                        // Aquí recibes la decisión
                        if (authorized) {
                            log.info("El usuario AUTORIZÓ el servidor: XD");
                            // Ejecutar lógica de autorización o envío de CustomPayload al backend
                        } else {
                            log.info("El usuario eligió EN OTRO MOMENTO.");
                        }
                    }
            ));
        });
    }
}
