package com.example.eshop.payment;

import com.example.eshop.payment.bakong.config.JacksonConfig;
import com.example.eshop.payment.dto.response.PaymentTransactionResponse;
import com.example.eshop.common.dto.APIResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentJsonTest {
    @Test void paymentDatesProduceCompleteParseableJson() throws Exception {
        var mapper = new JacksonConfig().objectMapper(Jackson2ObjectMapperBuilder.json());
        var transaction = PaymentTransactionResponse.builder().id(1L)
                .amount(new BigDecimal("119")).status("PENDING")
                .createdAt(LocalDateTime.of(2026, 10, 5, 12, 0)).build();
        var json = mapper.writeValueAsString(transaction);
        assertThat(mapper.readTree(json).get("status").asText()).isEqualTo("PENDING");
        assertThat(mapper.readTree(json).get("createdAt")).isNotNull();
    }

    @Test void securityErrorsProduceCompleteParseableJson() throws Exception {
        var mapper = new JacksonConfig().objectMapper(Jackson2ObjectMapperBuilder.json());
        var json = mapper.writeValueAsString(APIResponse.error("Unauthorized", 401));
        assertThat(mapper.readTree(json).get("status").asInt()).isEqualTo(401);
        assertThat(mapper.readTree(json).get("timestamp")).isNotNull();
    }
}