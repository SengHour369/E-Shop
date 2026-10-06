package com.example.eshop.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** One-way database reference for a randomly generated refresh credential. */
public final class RefreshTokenHash {
    private RefreshTokenHash() {}

    public static String of(String token) {
        if (token == null || token.isBlank()) {
            throw new com.example.eshop.common.exception.CustomMessageException("Refresh token is required", "401");
        }
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
