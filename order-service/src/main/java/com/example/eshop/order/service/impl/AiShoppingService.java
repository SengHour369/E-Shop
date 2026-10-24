package com.example.eshop.order.service.impl;

import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.order.automation.CheckoutAddressVerifier;
import com.example.eshop.order.dto.request.CartRequest;
import com.example.eshop.order.dto.request.OrderRequest;
import com.example.eshop.order.model.AiMutationReceipt;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.service.CartService;
import com.example.eshop.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

/** Fixed shopping adapters reuse domain services. The caller never supplies a customer identity or price. */
@Service
@RequiredArgsConstructor
public class AiShoppingService {

    private final CartService carts;
    private final CartRepository cartRows;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final AiMutationReceiptRepository receipts;
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final ObjectProvider<CheckoutAddressVerifier> addresses;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    public JsonNode cart() {
        return mapper.valueToTree(
                carts.getOrCreateCart(CurrentActor.userId()).object()
        );
    }

    @Transactional
    public JsonNode mutate(
            UUID key,
            String action,
            Long skuId,
            Long cartItemId,
            Long quantity
    ) {
        long user = CurrentActor.userId();

        if (!java.util.Set.of("add", "update", "remove").contains(action)
                || (!"remove".equals(action)
                && (skuId == null
                || skuId < 1
                || quantity == null
                || quantity < 1
                || quantity > 999))
                || (!"add".equals(action)
                && (cartItemId == null || cartItemId < 1))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid cart operation."
            );
        }

        carts.getOrCreateCart(user);
        var cart = cartRows.lockByUserId(user).orElseThrow();

        if ("update".equals(action)
                && cart.getCartItems().stream().noneMatch(item ->
                item.getId().equals(cartItemId)
                        && item.getProductSkuId().equals(skuId))) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cart item unavailable."
            );
        }

        String id = user + ":" + key;
        String fingerprint = hash(
                action + ":" + skuId + ":" + cartItemId + ":" + quantity
        );
        var previous = receipts.findById(id);

        if (previous.isPresent()) {
            if (!fingerprint.equals(previous.get().getFingerprint())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Key belongs to another cart operation."
                );
            }

            return json(previous.get().getResponseJson());
        }

        var request = new CartRequest(skuId, quantity, null);
        var result = switch (action) {
            case "add" -> carts.addItemToCart(user, request);
            case "update" -> carts.updateCartItem(user, cartItemId, request);
            default -> carts.removeItemFromCart(user, cartItemId);
        };
        JsonNode response = mapper.valueToTree(result.object());
        var receipt = new AiMutationReceipt();
        receipt.setId(id);
        receipt.setFingerprint(fingerprint);
        receipt.setResponseJson(response.toString());
        receipts.saveAndFlush(receipt);
        return response;
    }

    @Transactional
    public JsonNode preview(long addressId, String method) {
        long user = CurrentActor.userId();

        if (!java.util.Set.of(
                "ABA",
                "BAKONG",
                "KHQR",
                "CASH_ON_DELIVERY"
        ).contains(method)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported payment method."
            );
        }

        var verifier = addresses.getIfAvailable();

        if (verifier == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Address verification is unavailable."
            );
        }

        verifier.require(user, addressId);
        var current = refreshCart();

        if (!current.path("items").isArray()
                || current.path("items").isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cart is empty."
            );
        }

        UUID reviewId = UUID.randomUUID();
        var review = mapper.createObjectNode()
                .put("reviewId", reviewId.toString())
                .put("addressId", addressId)
                .put("paymentMethod", method)
                .put("currency", "USD")
                .put("expiresInSeconds", 300);
        review.set("cart", current);
        review.put("cartFingerprint", fingerprint(current));
        redis.opsForValue().set(
                reviewKey(reviewId),
                review.toString(),
                Duration.ofMinutes(5)
        );
        return review;
    }

    public JsonNode review(UUID id) {
        String value = redis.opsForValue().get(reviewKey(id));

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Checkout review expired or unavailable."
            );
        }

        return json(value);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public JsonNode create(UUID reviewId) {
        long user = CurrentActor.userId();

        // The durable checkout key survives review expiry and process restarts.
        var existing = orders.findByUserIdAndCheckoutKey(
                user,
                reviewId.toString()
        );

        if (existing.isPresent()) {
            return mapper.valueToTree(
                    orderService.getOrderDetailByUserId(
                            user,
                            existing.get().getId()
                    ).object()
            );
        }

        JsonNode review = review(reviewId);
        JsonNode current = new org.springframework.transaction.support.TransactionTemplate(
                transactionManager
        ).execute(status -> refreshCart());

        if (!fingerprint(current).equals(
                fingerprint(review.path("cart"))
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cart or price changed. Review again before confirming."
            );
        }

        var request = OrderRequest.builder()
                .addressId(review.path("addressId").longValue())
                .paymentMethod(review.path("paymentMethod").asText())
                .currency("USD")
                .checkoutKey(reviewId.toString())
                .build();
        request.setExpectedTotal(
                current.path("total_price").decimalValue()
        );
        request.setExpectedCartFingerprint(
                review.path("cartFingerprint").asText()
        );
        return mapper.valueToTree(
                orderService.createOrderFromCart(user, request).object()
        );
    }

    private JsonNode refreshCart() {
        long user = CurrentActor.userId();
        carts.getOrCreateCart(user);
        return mapper.valueToTree(carts.getCartByUserId(user).object());
    }

    private String fingerprint(JsonNode cart) {
        var lines = new java.util.TreeMap<Long, String>();
        cart.path("items").forEach(item -> lines.put(
                item.path("id").longValue(),
                item.path("product_sku_id")
                        + ":"
                        + item.path("quantity")
                        + ":"
                        + item.path("total_price")
                        .decimalValue()
                        .stripTrailingZeros()
        ));
        return hash(
                cart.path("id")
                        + ":"
                        + cart.path("total_price")
                        .decimalValue()
                        .stripTrailingZeros()
                        + ":"
                        + lines
        );
    }

    private String reviewKey(UUID id) {
        return "ai:checkout-review:" + CurrentActor.userId() + ":" + id;
    }

    private JsonNode json(String value) {
        try {
            return mapper.reader()
                    .with(
                            com.fasterxml.jackson.databind.DeserializationFeature
                                    .USE_BIG_DECIMAL_FOR_FLOATS
                    )
                    .readTree(value);
        } catch (Exception invalid) {
            throw new IllegalStateException(
                    "Stored shopping response unavailable"
            );
        }
    }

    public static String cartFingerprint(
            com.example.eshop.order.model.Cart cart
    ) {
        var lines = new java.util.TreeMap<Long, String>();
        cart.getCartItems().forEach(item -> lines.put(
                item.getId(),
                item.getProductSkuId()
                        + ":"
                        + item.getQuantity()
                        + ":"
                        + item.getTotalPrice().stripTrailingZeros()
        ));
        return hash(
                cart.getId()
                        + ":"
                        + cart.getTotalPrice().stripTrailingZeros()
                        + ":"
                        + lines
        );
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest
                            .getInstance("SHA-256")
                            .digest(
                                    value.getBytes(
                                            java.nio.charset.StandardCharsets.UTF_8
                                    )
                            )
            );
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}