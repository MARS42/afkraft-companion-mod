package com.mars4two.ikang.client.core.screens;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class AuthorizeServerRequestScreen extends Screen {

    private final String serverName;
    private final Consumer<Boolean> callback;
    private final Screen parentScreen;
    private final Component message;

    public AuthorizeServerRequestScreen(String serverName, Consumer<Boolean> callback) {
        this(null, serverName, callback);
    }

    public AuthorizeServerRequestScreen(Screen parentScreen, String serverName, Consumer<Boolean> callback) {
        super(Component.literal("AFKraft: Autorizar servidor"));
        this.parentScreen = parentScreen;
        this.serverName = serverName;
        this.callback = callback;
        this.message = Component.literal("¿Autorizar el servidor \"")
                .append(Component.literal(this.serverName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\"?"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonsY = this.height / 2 + 15;

        // Botón principal: Autorizar
        Button authorizeButton = Button.builder(
                        Component.literal("Autorizar"),
                        button -> handleDecision(true)
                )
                .bounds(this.width / 2 - buttonWidth - 6, buttonsY, buttonWidth, buttonHeight)
                .build();

        // Botón secundario: En otro momento
        Button cancelButton = Button.builder(
                        Component.literal("En otro momento"),
                        button -> handleDecision(false)
                )
                .bounds(this.width / 2 + 6, buttonsY, buttonWidth, buttonHeight)
                .build();

        this.addRenderableWidget(authorizeButton);
        this.addRenderableWidget(cancelButton);
    }

    private void handleDecision(boolean authorized) {
        if (this.callback != null) {
            this.callback.accept(authorized);
        }
        this.minecraft.setScreenAndShow(this.parentScreen);
    }

    @Override
    public void onClose() {
        handleDecision(false);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int textY = this.height / 2 - 20;
        graphics.centeredText(this.font, message, this.width / 2, textY, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}