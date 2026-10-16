package com.example.eshop.order.automation;

import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CheckoutAddressVerifier {

    private final JwtProperties jwt;

    @Value("${automation.auth-url:${AUTHORIZATION_AUTH_URL:http://auth-service:8081}}")
    private String authUrl;

    public void require(long userId, Long id) {
        if (id == null || id <= 0) {
            throw new BusinessLogicException("Delivery address required");
        }

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        Boolean owned = RestClient.builder()
                .baseUrl(authUrl)
                .requestFactory(factory)
                .build()
                .get()
                .uri(
                        "/internal/commerce/addresses/{id}/owner/{userId}",
                        id,
                        userId
                )
                .header(
                        "Authorization",
                        ServiceIdentity.bearer(
                                jwt,
                                "order-service",
                                "SERVICE_ORDER"
                        )
                )
                .header("X-Request-Id", RequestIds.current())
                .retrieve()
                .body(Boolean.class);

        if (!Boolean.TRUE.equals(owned)) {
            throw new BusinessLogicException(
                    "Delivery address does not belong to the customer"
            );
        }
    }
}