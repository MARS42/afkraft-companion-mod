package com.mars4two.ikang.client.platform.mc;

import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Isolates client-side GUI rendering, texture management, input events, permission checks,
 * and session access differences across Minecraft 1.21.1 and 26.2.
 */
public final class ClientUiAdapter {

    private ClientUiAdapter() {
    }

    public static boolean hasAdminPermission(PermissionSet permissions) {
        return permissions.hasPermission(Permissions.COMMANDS_ADMIN);
    }

    public static void showScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreenAndShow(screen);
    }

    public static boolean isCurrentScreen(Minecraft minecraft, Screen screen) {
        return minecraft.gui.screen() == screen;
    }

    public static void notifyClientChat(Minecraft minecraft, Component message) {
        minecraft.showDebugChat(message);
    }

    public static void sendPlayerMessage(Player player, Component message) {
        player.sendSystemMessage(message);
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
            minecraft.services().sessionService().joinServer(profileId, accessToken, serverId);
        } catch (com.mojang.authlib.exceptions.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    public static int getPixel(NativeImage image, int x, int y) {
        return image.getPixel(x, y);
    }

    public static void setPixel(NativeImage image, int x, int y, int color) {
        image.setPixel(x, y, color);
    }

    public static DynamicTexture registerDynamicTexture(
            Minecraft minecraft,
            Identifier textureId,
            String debugLabel,
            NativeImage image
    ) {
        DynamicTexture texture = new DynamicTexture(() -> debugLabel, image);
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

    public static boolean isEscapeKey(KeyEvent event) {
        return event.isEscape();
    }
}
