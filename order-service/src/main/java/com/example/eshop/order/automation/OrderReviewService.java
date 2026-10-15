package com.example.eshop.order.automation;

import com.example.eshop.common.automation.*;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.impl.CatalogAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class OrderReviewService {

    private final OrderRepository orders;
    private final PaymentAutomationClient payments;
    private final CatalogAccess catalog;
    private final JwtProperties jwt;
    private final AutomationEvents events;

    @Value("${automation.notification-url:http://notification-service:8086}")
    private String notificationUrl;

    @Transactional
    public Map<String, Object> review(long id) {
        return inspect(id, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> inspect(long id) {
        return inspect(id, false);
    }

    private Map<String, Object> inspect(long id, boolean persist) {
        var order = orders.findById(id).orElseThrow();
        var result = new LinkedHashMap<String, Object>();
        result.put("orderId", id);
        result.put("status", order.getStatus());
        result.put("amount", order.getTotalAmount());
        var findings = new ArrayList<String>();

        try {
            var reservation = catalog.snapshot(id);
            result.put("reservation", reservation);

            if (Set.of("PROCESSING", "SHIPPED", "DELIVERED")
                    .contains(order.getStatus())
                    && !"CONFIRMED".equals(reservation.status())) {
                findings.add("FULFILMENT_WITHOUT_CONFIRMED_STOCK");
            }

            if ("CANCELLED".equals(order.getStatus())
                    && !"RELEASED".equals(reservation.status())) {
                findings.add("CANCELLED_STOCK_NOT_RELEASED");
            }
        } catch (Exception ex) {
            findings.add("INVENTORY_UNAVAILABLE");
        }

        if (order.getPaymentId() != null) {
            try {
                var payment = payments.get(order.getPaymentId());
                result.put("payment", payment);

                if (payment.amount().compareTo(order.getTotalAmount()) != 0
                        || payment.verifiedAt() == null
                        || !"SUCCESS".equals(payment.status())) {
                    findings.add("PAYMENT_ORDER_INCONSISTENCY");
                }
            } catch (Exception ex) {
                findings.add("PAYMENT_UNAVAILABLE");
            }
        } else if (Set.of("PROCESSING", "SHIPPED", "DELIVERED")
                .contains(order.getStatus())
                && !"CASH_ON_DELIVERY".equals(order.getPaymentMethod())) {
            findings.add("FULFILMENT_WITHOUT_PAYMENT_EVIDENCE");
        }

        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(5000);

            Object deliveries = RestClient.builder()
                    .baseUrl(notificationUrl)
                    .requestFactory(factory)
                    .build()
                    .get()
                    .uri("/internal/commerce/notifications/{id}", id)
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
                    .retrieve()
                    .body(Object.class);

            result.put(
                    "notificationDeliveries",
                    deliveries == null ? List.of() : deliveries
            );
        } catch (Exception ex) {
            findings.add("NOTIFICATION_DELIVERY_UNAVAILABLE");
        }

        if (persist) {
            for (String code : findings) {
                events.review(
                        id,
                        code,
                        "Authoritative service review found "
                                + code
                                + ". Human review required; no financial state was changed."
                );
            }
        }

        result.put("findings", findings);
        result.put("financialChangesAllowed", false);
        return result;
    }
}