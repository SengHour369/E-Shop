package com.example.eshop.order.automation;

import com.example.eshop.common.automation.*;
import com.example.eshop.common.jwt.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class PaymentAutomationClient {

    private final RestClient client;
    private final JwtProperties jwt;

    public PaymentAutomationClient(
            JwtProperties jwt,
            @Value("${automation.payment-url:http://payment-service:8084}") String url
    ) {
        this.jwt = jwt;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public VerifiedPaymentSnapshot get(long id) {
        return client.get()
                .uri("/internal/commerce/payments/{id}", id)
                .header(
                        "Authorization",
                        ServiceIdentity.bearer(
                                jwt,
                                "order-service",
                                "SERVICE_ORDER"
                        )
                )
                .header(
                        "X-Request-Id",
                        com.example.eshop.common.request.RequestIds.current()
                )
                .headers(headers -> {
                    String trace = org.slf4j.MDC.get("traceId");

                    if (trace != null && trace.matches("[a-f0-9]{32}")) {
                        headers.set(
                                "traceparent",
                                "00-"
                                        + trace
                                        + "-"
                                        + java.util.UUID.randomUUID()
                                        .toString()
                                        .replace("-", "")
                                        .substring(0, 16)
                                        + "-01"
                        );
                    }
                })
                .retrieve()
                .body(VerifiedPaymentSnapshot.class);
    }
}