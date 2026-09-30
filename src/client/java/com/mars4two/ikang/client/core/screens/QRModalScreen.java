package com.mars4two.ikang.client.core.screens;

import com.mars4two.ikang.client.core.util.QRGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QRModalScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-QRModalScreen");

    private final String url;
    private final int qrSize = 196;
    private DynamicTexture qrTexture;
    private Identifier qrTextureId;
    private volatile Runnable onClosed = () -> {};
    private volatile Runnable onCancel = () -> {};
    private boolean showQr = false;
    private final Component allowShowQrMessage1;
    private final Component allowShowQrMessage2;

    public QRModalScreen(String url) {
        super(Component.literal("AFKraft: Conectar cuenta"));
        this.url = url;
        this.allowShowQrMessage1 = Component.literal("Estás a punto de mostrar el código QR para vincular tu cuenta.");
        this.allowShowQrMessage2 = Component.literal("¡Asegúrate de que sólo tú puedas verlo!")
                .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
    }

    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        super.init();

        showQr = false;

        Identifier obsidian = Identifier.withDefaultNamespace( "textures/block/obsidian.png");
        Identifier ironBlock = Identifier.withDefaultNamespace("textures/block/iron_block.png");

        var nativeImage = QRGenerator.generateBlockyQR(this.url, obsidian, ironBlock);
        if (nativeImage != null) {
            this.qrTexture = new DynamicTexture(() -> url, nativeImage);
            this.qrTextureId = Identifier.fromNamespaceAndPath("afkraft","connect_qr_code");
            Minecraft.getInstance().getTextureManager().register(this.qrTextureId, this.qrTexture);
            LOGGER.info("QR code image generated and registered successfully");
        }

        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonsY = this.height / 2 + 15;

        Button cancelConnectButton = Button.builder(
                        Component.literal("Cancelar"),
                        button -> onCancel()
                )
                .bounds(this.width / 2 - (buttonWidth / 2), this.height - (buttonHeight + 4), buttonWidth, buttonHeight)
                .build();

        Button cancelButton = Button.builder(
                        Component.literal("En otro momento"),
                        button -> onClose()
                )
                .bounds(this.width / 2 + 6, buttonsY, buttonWidth, buttonHeight)
                .build();

        Button authorizeButton = Button.builder(
                        Component.literal("Mostrar QR"),
                        button -> {
                            showQr = true;
                            this.removeWidget(button);
                            this.removeWidget(cancelButton);
                            this.addRenderableWidget(cancelConnectButton);
                        }
                )
                .bounds(this.width / 2 - buttonWidth - 6, buttonsY, buttonWidth, buttonHeight)
                .build();

        this.addRenderableWidget(authorizeButton);
        this.addRenderableWidget(cancelButton);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        if(!showQr) {
            graphics.centeredText(this.font, allowShowQrMessage1, this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
            graphics.centeredText(this.font, allowShowQrMessage2, this.width / 2, this.height / 2 - 20, 0xFF00FF00);
            return;
        }

        if (this.qrTextureId != null) {
            int x = (this.width - qrSize) / 2;
            int y = (this.height - qrSize) / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.qrTextureId, x, y, 0, 0, qrSize, qrSize, qrSize, qrSize, qrSize, qrSize);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.qrTexture != null) {
            this.qrTexture.close();
            Minecraft.getInstance().getTextureManager().release(this.qrTextureId);
            LOGGER.info("QR code image released successfully");
        }
        this.onClosed.run();
    }

    private void onCancel() {
        this.onCancel.run();
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}