package com.example.learning_spring_security.validation;

import com.example.learning_spring_security.dto.Request.CreateReturnRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import jakarta.validation.ConstraintViolation;

public class RequiredIfValidatorTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    public static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    public static void teardown() {
        factory.close();
    }

    @Test
    public void whenReturnTypeIsRefund_andAmountNull_thenViolation() {
        CreateReturnRequest req = CreateReturnRequest.builder()
                .orderId(1L)
                .customerId(2L)
                .productId(3L)
                .returnType("REFUND")
                .reason("defective")
                .amount(null)
                .build();

        Set<ConstraintViolation<CreateReturnRequest>> violations = validator.validate(req);
        boolean hasAmountViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));

        Assertions.assertTrue(hasAmountViolation, "Expected validation error on amount when returnType=REFUND");
    }
}
