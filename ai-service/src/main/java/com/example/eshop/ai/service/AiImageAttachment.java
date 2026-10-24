package com.example.eshop.ai.service;

import com.example.eshop.ai.enums.AiExecutionStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.function.Supplier;

/** An attachment exists only during its authenticated upload request, never in model output or Redis. */
@Component
public class AiImageAttachment {

    private record Attached(UUID id, MultipartFile image) {}

    private final ThreadLocal<Attached> attachment = new ThreadLocal<>();

    public <T> T with(UUID id, MultipartFile image, Supplier<T> work) {
        if (attachment.get() != null) {
            throw new IllegalStateException("Nested image attachment");
        }

        attachment.set(new Attached(id, image));

        try {
            return work.get();
        } finally {
            attachment.remove();
        }
    }

    public MultipartFile require(String id) {
        var current = attachment.get();

        if (current == null || !current.id().toString().equals(id)) {
            throw new AiFailure(
                    "IMAGE_ATTACHMENT_REQUIRED",
                    AiExecutionStatus.NEEDS_INPUT,
                    "Attach a product photo using the camera or upload button."
            );
        }

        return current.image();
    }

    public String fingerprint(MultipartFile image) {
        if (image == null
                || image.isEmpty()
                || image.getSize() > 8 * 1024 * 1024
                || image.getContentType() == null
                || !java.util.Set.of(
                "image/jpeg",
                "image/png",
                "image/webp"
        ).contains(image.getContentType())) {
            throw new AiFailure(
                    "INVALID_IMAGE",
                    AiExecutionStatus.NEEDS_INPUT,
                    "Choose a JPEG, PNG or WebP image up to 8 MB."
            );
        }

        try (var input = image.getInputStream()) {
            byte[] bytes = input.readNBytes(8 * 1024 * 1024 + 1);

            if (bytes.length > 8 * 1024 * 1024) {
                throw new IllegalArgumentException("Image too large");
            }

            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest
                            .getInstance("SHA-256")
                            .digest(bytes)
            );
        } catch (Exception invalid) {
            throw new AiFailure(
                    "INVALID_IMAGE",
                    AiExecutionStatus.NEEDS_INPUT,
                    "The product photo could not be read."
            );
        }
    }
}