package com.mars4two.ikang.client.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QRGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("AFKraft-QRGenerator");

    private QRGenerator() {
        // Utility class
    }

    public static NativeImage generateBlockyQR(String url, Identifier blackBlockPath, Identifier whiteBlockPath) {
        try {
            LOGGER.info("Generating QR code...");
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 1, 1, hints);

            int modules = matrix.getWidth();

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

                // 3. Iterar sobre la matriz del QR y estampar las texturas bloque por bloque
                for (int row = 0; row < modules; row++) {
                    for (int col = 0; col < modules; col++) {
                        boolean isBlack = matrix.get(col, row);
                        NativeImage sourceTexture = isBlack ? blackImg : whiteImg;

                        // Copiamos la sub-matriz de 16x16 píxeles del bloque al lienzo del QR
                        for (int x = 0; x < textureSize; x++) {
                            for (int y = 0; y < textureSize; y++) {
                                int color = sourceTexture.getPixel(x, y);

                                // Calculamos la posición exacta en la textura final
                                int targetX = (col * textureSize) + x;
                                int targetY = (row * textureSize) + y;

                                finalQR.setPixel(targetX, targetY, color);
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