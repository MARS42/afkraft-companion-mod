package com.mars4two.ikang.client.core.screens;

import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import net.minecraft.ChatFormatting;
//? if >=26.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

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

        Button authorizeButton = Button.builder(
                        Component.literal("Autorizar"),
                        button -> handleDecision(true)
                )
                .bounds(this.width / 2 - buttonWidth - 6, buttonsY, buttonWidth, buttonHeight)
                .build();

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
        ClientUiAdapter.showScreen(this.minecraft, this.parentScreen);
    }

    @Override
    public void onClose() {
        handleDecision(false);
    }

    //? if >=26.2 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int textY = this.height / 2 - 20;
        ClientUiAdapter.drawCenteredText(graphics, this.font, message, this.width / 2, textY, 0xFFFFFFFF);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);

        int textY = this.height / 2 - 20;
        ClientUiAdapter.drawCenteredText(graphics, this.font, message, this.width / 2, textY, 0xFFFFFFFF);
    }
    *///?}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
