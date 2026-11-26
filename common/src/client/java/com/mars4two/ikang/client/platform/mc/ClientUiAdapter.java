package com.mars4two.ikang.client.platform.mc;

import com.mars4two.ikang.core.auth.AuthenticationException;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Isolates client-side GUI rendering, texture management, input events, permission checks,
 * and session access differences across Minecraft 1.21.1 and 26.3.
 */
public final class ClientUiAdapter {

    //? if >=1.21.9 {
    private static volatile KeyMapping.Category keyCategory;
    //?}

    private ClientUiAdapter() {
    }

    public static KeyMapping createKeyMapping(String translationKey, int defaultKeyCode) {
        //? if >=1.21.9 {
        KeyMapping.Category category = keyCategory;
        if (category == null) {
            category = KeyMapping.Category.register(WorldAndEntityAdapter.modId("general"));
            keyCategory = category;
        }
        return new KeyMapping(translationKey, defaultKeyCode, category);
        //?} else {
        /*return new KeyMapping(translationKey, defaultKeyCode, AFKraftTranslations.KEY_CATEGORY);
        *///?}
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

    public static Screen currentScreen(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }

    public static boolean isCurrentScreen(Minecraft minecraft, Screen screen) {
        return currentScreen(minecraft) == screen;
    }

    public static void setClipboard(Minecraft minecraft, String text) {
        if (minecraft != null && minecraft.keyboardHandler != null && text != null) {
            minecraft.keyboardHandler.setClipboard(text);
        }
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

    public static void drawCenteredText(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int centerX,
            int y,
            int color
    ) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void drawText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y,
            int color
    ) {
        graphics.text(font, text, x, y, color);
    }

    public static void renderTooltip(
            GuiGraphicsExtractor graphics,
            Font font,
            List<? extends FormattedCharSequence> lines,
            int mouseX,
            int mouseY
    ) {
        graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    public static void renderWidget(
            GuiGraphicsExtractor graphics,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
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

    public static void fillRect(
            GuiGraphicsExtractor graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int color
    ) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public static void drawOutline(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.outline(x, y, width, height, color);
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

    public static void drawCenteredText(
            GuiGraphics graphics,
            Font font,
            FormattedCharSequence text,
            int centerX,
            int y,
            int color
    ) {
        graphics.drawCenteredString(font, text, centerX, y, color);
    }

    public static void drawText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int color
    ) {
        graphics.drawString(font, text, x, y, color);
    }

    public static void renderTooltip(
            GuiGraphics graphics,
            Font font,
            List<? extends FormattedCharSequence> lines,
            int mouseX,
            int mouseY
    ) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        //?} else {
        graphics.renderTooltip(font, lines, mouseX, mouseY);
        //?}
    }

    public static void renderWidget(
            GuiGraphics graphics,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        widget.render(graphics, mouseX, mouseY, partialTick);
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

    public static void fillRect(
            GuiGraphics graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int color
    ) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public static void drawOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
    *///?}

    //? if >=1.21.9 {
    public static boolean isEscapeKey(KeyEvent event) {
        return event.isEscape();
    }

    public static boolean isLeftKey(KeyEvent event) {
        return event.isLeft();
    }

    public static boolean isRightKey(KeyEvent event) {
        return event.isRight();
    }

    public static boolean isUpKey(KeyEvent event) {
        return event.isUp();
    }

    public static boolean isDownKey(KeyEvent event) {
        return event.isDown();
    }

    public static boolean isShiftDown(KeyEvent event) {
        return event.hasShiftDown();
    }
    //?} else {
    /*public static boolean isEscapeKey(int keyCode) {
        return keyCode == 256;
    }

    public static boolean isLeftKey(int keyCode) {
        return keyCode == InputConstants.KEY_LEFT;
    }

    public static boolean isRightKey(int keyCode) {
        return keyCode == InputConstants.KEY_RIGHT;
    }

    public static boolean isUpKey(int keyCode) {
        return keyCode == InputConstants.KEY_UP;
    }

    public static boolean isDownKey(int keyCode) {
        return keyCode == InputConstants.KEY_DOWN;
    }

    public static boolean isShiftDown(int modifiers) {
        return (modifiers & 1) != 0;
    }
    *///?}
}
