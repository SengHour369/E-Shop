package com.example.eshop.payment.automation;

import com.example.eshop.payment.bakong.dto.BakongResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

/** Called only on the response fetched server-to-server for our stored QR MD5. */
public final class BakongVerificationPolicy {

    private BakongVerificationPolicy() {}

    public static String verifiedHash(
            BakongResponse response,
            BigDecimal amount,
            String currency,
            String recipient,
            ObjectMapper mapper
    ) {
        if (response == null
                || response.getResponseCode() != 0
                || response.getErrorCode() != null
                && response.getErrorCode() != 0) {
            return null;
        }

        var data = mapper.valueToTree(response.getData());

        if (data == null || !data.isObject()) {
            throw new IllegalStateException(
                    "Provider success missing transaction evidence"
            );
        }

        String hash = data.path("hash").asText("");

        if (!hash.matches("[a-fA-F0-9]{64}")
                || !currency.equals(data.path("currency").asText())
                || !recipient.equals(data.path("toAccountId").asText())) {
            throw new IllegalStateException("Provider evidence mismatch");
        }

        try {
            if (new BigDecimal(data.path("amount").asText())
                    .compareTo(amount) != 0) {
                throw new IllegalStateException(
                        "Provider amount mismatch"
                );
            }
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid provider amount");
        }

        return hash.toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean notFound(BakongResponse response) {
        return response != null
                && response.getResponseCode() == 1
                && Integer.valueOf(1).equals(response.getErrorCode());
    }
}