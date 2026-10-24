package com.example.eshop.payment.bakong;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.Map;

/** Branded presentation of an existing authoritative KHQR payload; never changes payment data. */
public final class BakongQrRenderer {

    private BakongQrRenderer() {}

    public static byte[] render(String payload)
            throws IOException, WriterException {
        var matrix = new QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                300,
                300,
                Map.of(
                        EncodeHintType.CHARACTER_SET, "UTF-8",
                        EncodeHintType.ERROR_CORRECTION,
                        ErrorCorrectionLevel.H,
                        EncodeHintType.MARGIN, 4
                )
        );
        var image = new java.awt.image.BufferedImage(
                300,
                300,
                java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        var background = image.createGraphics();

        try {
            background.drawImage(
                    MatrixToImageWriter.toBufferedImage(matrix),
                    0,
                    0,
                    null
            );
        } finally {
            background.dispose();
        }

        // A small central badge retains the finder patterns and four-module quiet zone.
        try (var resource = BakongQrRenderer.class.getResourceAsStream(
                "/images/bakong-logo.png"
        )) {
            if (resource == null) {
                throw new IOException("Bakong logo resource unavailable");
            }

            var logo = ImageIO.read(resource);

            if (logo == null) {
                throw new IOException("Bakong logo resource invalid");
            }

            var g = image.createGraphics();

            try {
                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                g.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(
                        126,
                        126,
                        48,
                        48,
                        12,
                        12
                ));
                g.setClip(new RoundRectangle2D.Double(
                        132,
                        132,
                        36,
                        36,
                        6,
                        6
                ));
                g.drawImage(logo, 132, 132, 36, 36, null);
            } finally {
                g.dispose();
            }
        }

        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        }
    }
}