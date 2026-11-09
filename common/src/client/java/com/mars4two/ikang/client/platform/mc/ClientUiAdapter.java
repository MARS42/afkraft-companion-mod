package com.mars4two.ikang.client.platform.mc;

import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} elif >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}
//? if >=1.21.11 {
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Isolates client-side GUI rendering, texture management, input events, permission checks,
 * and session access differences across Minecraft 1.21.1 and 26.2.
 */
public final class ClientUiAdapter {

    private ClientUiAdapter() {
    }

    //? if >=1.21.11 {
    public static boolean hasAdminPermission(PermissionSet permissions) {
        return permissions.hasPermission(Permissions.COMMANDS_ADMIN);
    }
    //?}

    public static void showScreen(Minecraft minecraft, Screen screen) {
        //? if >=1.21.9 {
        minecraft.setScreenAndShow(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }

    public static boolean isCurrentScreen(Minecraft minecraft, Screen screen) {
        //? if >=26.2 {
        return minecraft.gui.screen() == screen;
        //?} else {
        /*return minecraft.screen == screen;
        *///?}
    }

    public static void notifyClientChat(Minecraft minecraft, Component message) {
        //? if >=26.2 {
        minecraft.showDebugChat(message);
        //?} elif >=26.1 {
        /*minecraft.gui.getChat().addClientSystemMessage(message);
        *///?} else {
        /*minecraft.gui.getChat().addMessage(message);
        *///?}
    }

    public static void sendPlayerMessage(Player player, Component message) {
        //? if >=26.1 {
        player.sendSystemMessage(message);
        //?} else {
        /*player.displayClientMessage(message, false);
        *///?}
    }

    public static UUID clientProfileId(Minecraft minecraft) {
        return minecraft.getUser().getProfileId();
    }

    public static String clientUserName(Minecraft minecraft) {
        return minecraft.getUser().getName();
    }

    public static String clientAccessToken(Minecraft minecraft) {
        return minecraft.getUser().getAccessToken();
    }

    public static void joinMojangSession(Minecraft minecraft, UUID profileId, String accessToken, String serverId)
            throws AuthenticationException {
        try {
            //? if >=1.21.9 {
            minecraft.services().sessionService().joinServer(profileId, accessToken, serverId);
            //?} else {
            /*minecraft.getMinecraftSessionService().joinServer(profileId, accessToken, serverId);
            *///?}
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    public static int getPixel(NativeImage image, int x, int y) {
        //? if >=1.21.2 {
        return image.getPixel(x, y);
        //?} else {
        /*return image.getPixelRGBA(x, y);
        *///?}
    }

    public static void setPixel(NativeImage image, int x, int y, int color) {
        //? if >=1.21.2 {
        image.setPixel(x, y, color);
        //?} else {
        /*image.setPixelRGBA(x, y, color);
        *///?}
    }

    public static DynamicTexture registerDynamicTexture(
            Minecraft minecraft,
            Identifier textureId,
            String debugLabel,
            NativeImage image
    ) {
        //? if >=1.21.5 {
        DynamicTexture texture = new DynamicTexture(() -> debugLabel, image);
        //?} else {
        /*DynamicTexture texture = new DynamicTexture(image);
        *///?}
        minecraft.getTextureManager().register(textureId, texture);
        return texture;
    }

    public static void releaseDynamicTexture(
            Minecraft minecraft,
            Identifier textureId,
            DynamicTexture texture
    ) {
        texture.close();
        minecraft.getTextureManager().release(textureId);
    }

    //? if >=26.1 {
    public static void drawCenteredText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void blitQrTexture(
            GuiGraphicsExtractor graphics,
            Identifier textureId,
            int x,
            int y,
            int size
    ) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0, 0, size, size, size, size, size, size);
    }
    //?} else {
    /*public static void drawCenteredText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        graphics.drawCenteredString(font, text, centerX, y, color);
    }

    public static void blitQrTexture(
            GuiGraphics graphics,
            Identifier textureId,
            int x,
            int y,
            int size
    ) {
        //? if >=1.21.6 {
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0F, 0.0F, size, size, size, size);
        //?} elif >=1.21.2 {
        graphics.blit(RenderType::guiTextured, textureId, x, y, 0.0F, 0.0F, size, size, size, size);
        //?} else {
        graphics.blit(textureId, x, y, 0.0F, 0.0F, size, size, size, size);
        //?}
    }
    *///?}

    //? if >=1.21.9 {
    public static boolean isEscapeKey(KeyEvent event) {
        return event.isEscape();
    }
    //?} else {
    /*public static boolean isEscapeKey(int keyCode) {
        return keyCode == 256;
    }
    *///?}
}
