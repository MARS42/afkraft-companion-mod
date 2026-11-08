package com.mars4two.ikang.client.core.util;

import com.mars4two.ikang.client.platform.mc.ClientUiAdapter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

public final class QRGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-QRGenerator");

    private QRGenerator() {
    }

    public static NativeImage generateBlockyQR(String url, Identifier blackBlockPath, Identifier whiteBlockPath) {
        try {
            LOGGER.info("Generating QR code...");
            QRMatrixGenerator.QrBitGrid matrix = QRMatrixGenerator.encode(url);
            int modules = matrix.size();

            var resourceManager = Minecraft.getInstance().getResourceManager();

            Optional<Resource> blackResource = resourceManager.getResource(blackBlockPath);
            Optional<Resource> whiteResource = resourceManager.getResource(whiteBlockPath);

            if (blackResource.isEmpty() || whiteResource.isEmpty()) {
                throw new IllegalArgumentException("No se pudieron encontrar las texturas de los bloques especificados.");
            }

            LOGGER.info("Using black texture: {}", blackBlockPath);
            LOGGER.info("Using white texture: {}", whiteBlockPath);

            try (InputStream blackStream = blackResource.get().open();
                 InputStream whiteStream = whiteResource.get().open();
                 NativeImage blackImg = NativeImage.read(blackStream);
                 NativeImage whiteImg = NativeImage.read(whiteStream)) {

                int textureSize = blackImg.getWidth();
                int finalImageSize = modules * textureSize;

                NativeImage finalQR = new NativeImage(finalImageSize, finalImageSize, false);

                for (int row = 0; row < modules; row++) {
                    for (int col = 0; col < modules; col++) {
                        boolean isBlack = matrix.isDark(col, row);
                        NativeImage sourceTexture = isBlack ? blackImg : whiteImg;

                        for (int x = 0; x < textureSize; x++) {
                            for (int y = 0; y < textureSize; y++) {
                                int color = ClientUiAdapter.getPixel(sourceTexture, x, y);

                                int targetX = (col * textureSize) + x;
                                int targetY = (row * textureSize) + y;

                                ClientUiAdapter.setPixel(finalQR, targetX, targetY, color);
                            }
                        }
                    }
                }
                return finalQR;
            }
        } catch (Exception e) {
            LOGGER.error("Error generating QR code: {}", e.getMessage());
            return null;
        }
    }
}
