package com.example.learning_spring_security.utils;

import com.example.learning_spring_security.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class PaymentCodeGenerator {

    private static final String PREFIX = "PAY";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    private final PaymentRepository paymentRepository;


    /**
     * Generate unique payment code
     *
     * Example:
     * PAY-8D7F9A1K
     */
    public String generatePaymentCode() {

        String code;

        do {
            code = PREFIX + "-" + generateRandomPart();

        } while (paymentRepository.existsByCode(code));


        return code;
    }


    private String generateRandomPart() {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {

            int index = random.nextInt(CHARACTERS.length());

            builder.append(
                    CHARACTERS.charAt(index)
            );
        }

        return builder.toString();
    }
}