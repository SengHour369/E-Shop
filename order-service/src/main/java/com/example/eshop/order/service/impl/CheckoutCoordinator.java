package com.example.eshop.order.service.impl;

import com.example.eshop.common.dto.*;
import com.example.eshop.common.exception.*;
import com.example.eshop.common.security.CurrentCustomer;
import com.example.eshop.order.dto.request.OrderRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import com.example.eshop.order.mapper.*;
import com.example.eshop.order.model.*;
import com.example.eshop.order.repository.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class CheckoutCoordinator {
    private final OrderRepository orders;
    private final CartRepository carts;
    private final CatalogAccess catalog;
    private final OrderMapper mapper;
    private final TransactionTemplate transactions;

    public ResponseErrorTemplate create(Long userId, OrderRequest request) {
        CurrentCustomer.require(userId);
        if (request.getCurrency() != null && !"USD".equalsIgnoreCase(request.getCurrency()))
            throw new BusinessLogicException("Catalog prices use USD");
        String key = request.getCheckoutKey() == null || request.getCheckoutKey().isBlank()
            ? UUID.randomUUID().toString() : request.getCheckoutKey();
        if (key.length() > 100) throw new BusinessLogicException("checkout_key must not exceed 100 characters");
        Long id = transactions.execute(tx -> {
            Cart cart = carts.lockByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
            var existing = orders.findByUserIdAndCheckoutKey(userId, key);
            if (existing.isPresent()) {
                if (!Objects.equals(existing.get().getShippingAddressId(), request.getAddressId()))
                    throw new BusinessLogicException("Checkout key was already used for a different address");
                return existing.get().getId();
            }
            if (cart.getCheckoutOrderId() != null) return cart.getCheckoutOrderId();
            if (cart.getCartItems().isEmpty()) throw new BusinessLogicException("Cart is empty");
            OrderDetail order = OrderDetail.builder().userId(userId).orderNumber("ORD-" + UUID.randomUUID())
                .orderDate(LocalDateTime.now()).status("CHECKOUT_PENDING").totalAmount(BigDecimal.ZERO)
                .shippingAddressId(request.getAddressId()).checkoutKey(key).catalogCompleted(false).build();
            order.setOrderItems(cart.getCartItems().stream().map(i ->
                OrderItemMapper.toEntity(order, i.getProductSkuId(), i.getQuantity(), BigDecimal.ZERO)).toList());
            orders.saveAndFlush(order);
            cart.setCheckoutOrderId(order.getId());
            return order.getId();
        });
        try { process(id); }
        catch (RuntimeException ex) { defer(id, ex); }
        return transactions.execute(tx -> mapper.toResponse(orders.findByIdWithItems(id).orElseThrow()));
    }

    // The order row is the durable work item. Catalog operations are idempotent across crash/retry boundaries.
    public void process(Long id) {
        transactions.executeWithoutResult(tx -> {
            OrderDetail order = orders.lockById(id).orElseThrow();
            if ("CANCELLED".equals(order.getStatus()) && !order.isCatalogCompleted()) {
                catalog.release(id); order.setCatalogCompleted(true); return;
            }
            if (!"CHECKOUT_PENDING".equals(order.getStatus())) return;
            CheckoutRequest request = new CheckoutRequest(id, order.getUserId(), order.getOrderItems().stream()
                .map(i -> new CheckoutRequest.Line(i.getProductSkuId(), i.getQuantity())).toList());
            try { catalog.reserve(request); }
            catch (FeignException ex) {
                if (ex.status() == 400 || ex.status() == 404) {
                    order.setStatus("FAILED"); order.setCatalogCompleted(true);
                    Cart cart = carts.lockByUserId(order.getUserId()).orElseThrow();
                    if (Objects.equals(cart.getCheckoutOrderId(), id)) cart.setCheckoutOrderId(null);
                    return;
                }
                throw ex;
            }
            CheckoutResult result = catalog.confirm(id);
            Map<Long, CheckoutResult.Line> lines = new HashMap<>();
            result.items().forEach(l -> lines.put(l.productSkuId(), l));
            for (OrderItem item : order.getOrderItems()) {
                var line = lines.get(item.getProductSkuId());
                if (line == null || !line.quantity().equals(item.getQuantity()))
                    throw new IllegalStateException("Catalog checkout snapshot does not match order");
                item.setBaseUnitPrice(line.originalPrice()); item.setDiscountAmount(line.discountAmount());
                item.setFinalUnitPrice(line.finalPrice()); item.setUnitPrice(line.finalPrice());
                item.setPromotionId(line.promotionId()); item.setPromotionName(line.promotionName());
                item.setTotalPrice(line.finalPrice().multiply(BigDecimal.valueOf(line.quantity())));
            }
            order.setTotalAmount(result.total()); order.setStatus("PENDING"); order.setCatalogCompleted(true);
            Cart cart = carts.lockByUserId(order.getUserId()).orElseThrow();
            if (Objects.equals(cart.getCheckoutOrderId(), id)) {
                cart.getCartItems().clear(); cart.setTotalPrice(BigDecimal.ZERO);
                cart.setTotalItems(0); cart.setCheckoutOrderId(null);
            }
        });
    }

    @Scheduled(fixedDelayString = "${checkout.recovery-delay-ms:10000}")
    public void recover() {
        List<Long> pending = transactions.execute(tx -> orders.recoverable(LocalDateTime.now(), PageRequest.of(0, 50)));
        for (Long id : pending) {
            try { process(id); }
            catch (RuntimeException ex) { defer(id, ex); }
        }
    }

    private void defer(Long id, RuntimeException cause) {
        log.warn("Checkout recovery deferred for {}: {}", id, cause.getClass().getSimpleName());
        // Back off failed work so a persistently failing batch does not starve newer orders.
        transactions.executeWithoutResult(tx -> orders.lockById(id).ifPresent(order ->
            order.setCheckoutRetryAt(LocalDateTime.now().plusSeconds(30))));
    }
}

