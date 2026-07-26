package com.mars4two.ikang.client.screens;

import com.mars4two.ikang.client.core.util.QRGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public QRModalScreen(String url) {
        super(Component.literal("AFKraft: Conectar cuenta"));
        this.url = url;
    }

    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    @Override
    protected void init() {
        super.init();

        Identifier obsidian = Identifier.withDefaultNamespace( "textures/block/obsidian.png");
        Identifier ironBlock = Identifier.withDefaultNamespace("textures/block/iron_block.png");

        var nativeImage = QRGenerator.generateBlockyQR(this.url, obsidian, ironBlock);
        if (nativeImage != null) {
            this.qrTexture = new DynamicTexture(() -> url, nativeImage);
            this.qrTextureId = Identifier.fromNamespaceAndPath("afkraft","connect_qr_code");
            Minecraft.getInstance().getTextureManager().register(this.qrTextureId, this.qrTexture);
            LOGGER.info("QR code image generated and registered successfully");
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}