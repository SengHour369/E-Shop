package com.example.eshop.catalog.scanner;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import org.springframework.web.multipart.MultipartFile;

/** Decode a sampled image from disk, never an upload-sized byte array. */
public final class ScanImageNormalizer {

    private ScanImageNormalizer() {}

    public static byte[] normalize(MultipartFile file) throws IOException {
        try (var source = file.getInputStream();
             var input = new FileCacheImageInputStream(source, null)) {
            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IOException("Unsupported image encoding");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();

                if (!format.equalsIgnoreCase("JPEG")
                        && !format.equalsIgnoreCase("PNG")
                        && !format.equalsIgnoreCase("WebP")) {
                    throw new IOException("Unsupported image encoding");
                }

                int width = reader.getWidth(0), height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    throw new IOException("Invalid image dimensions");
                }

                int sample = (int) Math.max(
                        1,
                        (Math.max((long) width, height) + 1023) / 1024
                );
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, parameters);

                if (decoded == null
                        || decoded.getWidth() > 1024
                        || decoded.getHeight() > 1024) {
                    throw new IOException("Could not sample image");
                }

                try {
                    int edge = 1024;

                    while (edge >= 128) {
                        double scale = Math.min(
                                1d,
                                (double) edge
                                        / Math.max(
                                        decoded.getWidth(),
                                        decoded.getHeight()
                                )
                        );
                        BufferedImage rgb = new BufferedImage(
                                Math.max(1, (int) (decoded.getWidth() * scale)),
                                Math.max(1, (int) (decoded.getHeight() * scale)),
                                BufferedImage.TYPE_INT_RGB
                        );

                        try {
                            var graphics = rgb.createGraphics();

                            try {
                                graphics.setColor(Color.WHITE);
                                graphics.fillRect(
                                        0,
                                        0,
                                        rgb.getWidth(),
                                        rgb.getHeight()
                                );
                                graphics.drawImage(
                                        decoded,
                                        0,
                                        0,
                                        rgb.getWidth(),
                                        rgb.getHeight(),
                                        null
                                );
                            } finally {
                                graphics.dispose();
                            }

                            var output = new ByteArrayOutputStream();

                            if (!ImageIO.write(rgb, "jpeg", output)) {
                                throw new IOException(
                                        "JPEG encoder unavailable"
                                );
                            }

                            if (output.size() <= 1_500_000) {
                                return output.toByteArray();
                            }
                        } finally {
                            rgb.flush();
                        }

                        edge /= 2;
                    }

                    throw new IOException(
                            "Could not prepare image for recognition"
                    );
                } finally {
                    decoded.flush();
                }
            } finally {
                reader.dispose();
            }
        }
    }
}