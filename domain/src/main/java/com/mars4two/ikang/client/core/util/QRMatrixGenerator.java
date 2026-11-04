package com.mars4two.ikang.client.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Map;
import java.util.Objects;

/**
 * Pure domain QR code encoder encapsulating ZXing behind an immutable boolean module grid.
 */
public final class QRMatrixGenerator {

    public record QrBitGrid(int size, boolean[] modules) {
        public QrBitGrid {
            if (size <= 0) {
                throw new IllegalArgumentException("QR grid size must be positive");
            }
            Objects.requireNonNull(modules, "modules");
            if (modules.length != size * size) {
                throw new IllegalArgumentException("QR modules array length must equal size * size");
            }
            modules = modules.clone();
        }

        public boolean isDark(int col, int row) {
            return modules[row * size + col];
        }
    }

    private QRMatrixGenerator() {
        // Utility class
    }

    public static QrBitGrid encode(String content) {
        Objects.requireNonNull(content, "content");
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 1, 1, hints);
            int size = matrix.getWidth();
            boolean[] modules = new boolean[size * size];
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    modules[row * size + col] = matrix.get(col, row);
                }
            }
            return new QrBitGrid(size, modules);
        } catch (WriterException e) {
            throw new IllegalArgumentException("Failed to encode QR matrix", e);
        }
    }
}
