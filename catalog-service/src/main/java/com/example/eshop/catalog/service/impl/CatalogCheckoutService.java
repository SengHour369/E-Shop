package com.example.eshop.catalog.service.impl;

import com.example.eshop.common.dto.*;
import com.example.eshop.common.exception.*;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CatalogCheckoutService {
    private final CheckoutReservationRepository reservations;
    private final ProductSkuRepository skus;
    private final PromotionSkuRepository assignments;
    private final PromotionRepository promotions;
    private final PromotionUsageRepository usages;
    private final InventoryRepository inventories;
    private final StockMovementRepository movements;
    private final ObjectMapper json;
    private final Clock clock;

    @Transactional
    public CheckoutResult reserve(CheckoutRequest request) {
        validate(request);
        var existing = reservations.lockById(request.orderId());
        if (existing.isPresent()) {
            CheckoutReservation r = existing.get();
            CheckoutResult result = read(r);
            if (!r.getUserId().equals(request.userId()) || !quantities(result).equals(quantities(request)))
                throw new BusinessLogicException("Checkout key was already used for different items");
            if ("RELEASED".equals(r.getStatus())) throw new BusinessLogicException("Checkout was released");
            return result;
        }
        CheckoutResult priced = price(request, true);
        CheckoutReservation r = new CheckoutReservation();
        r.setOrderId(request.orderId()); r.setUserId(request.userId()); r.setStatus("RESERVED");
        r.setSnapshot(write(priced));
        reservations.saveAndFlush(r);
        var inventory = lockInventory(priced);
        for (var line : priced.items()) {
            Inventory i = inventory.get(line.productSkuId());
            if (i == null || i.getQuantity() - i.getReservedQuantity() < line.quantity())
                throw new BusinessLogicException("Insufficient stock for SKU " + line.productSkuId());
            i.setReservedQuantity(Math.addExact(i.getReservedQuantity(), line.quantity()));
            i.setAvailableQuantity(i.getQuantity() - i.getReservedQuantity());
        }
        Map<Long, BigDecimal> discounts = new TreeMap<>();
        for (var line : priced.items()) if (line.promotionId() != null)
            discounts.merge(line.promotionId(), line.discountAmount().multiply(BigDecimal.valueOf(line.quantity())), BigDecimal::add);
        for (var entry : discounts.entrySet()) {
            PromotionUsage usage = new PromotionUsage();
            usage.setPromotion(promotions.getReferenceById(entry.getKey()));
            usage.setOrderId(request.orderId()); usage.setUserId(request.userId());
            usage.setDiscountAmount(entry.getValue()); usage.setUsedAt(LocalDateTime.now(clock));
            usages.save(usage);
        }
        return priced;
    }

    @Transactional(readOnly = true)
    public CheckoutResult quote(CheckoutRequest request) {
        validate(request);
        return price(request, false);
    }

    private CheckoutResult price(CheckoutRequest request, boolean lock) {
        Map<Long, Long> quantities = quantities(request);
        List<ProductSku> skuList = skus.findAllById(quantities.keySet());
        if (skuList.size() != quantities.size()) throw new ResourceNotFoundException("One or more SKUs do not exist");
        for (ProductSku sku : skuList)
            if (!Boolean.TRUE.equals(sku.getProduct().getIsActive()) || Boolean.TRUE.equals(sku.getProduct().getDeleted()))
                throw new BusinessLogicException("SKU is not sellable: " + sku.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        List<PromotionSku> links = assignments.eligible(quantities.keySet(), now);
        List<Long> ids = links.stream().map(a -> a.getPromotion().getId()).distinct().sorted().toList();
        if (lock && !ids.isEmpty()) {
            // Refresh locked entities so updates committed while waiting cannot leave stale terms in the persistence context.
            promotions.lockAll(ids).forEach(p -> entityManager.refresh(p, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
            now = LocalDateTime.now(clock);
            // Assignment removal can commit while we wait for the promotion lock.
            Set<Long> lockedIds = new HashSet<>(ids);
            links = assignments.eligible(quantities.keySet(), now).stream()
                .filter(a -> lockedIds.contains(a.getPromotion().getId())).toList();
        }
        Map<Long, Boolean> eligible = new HashMap<>();
        for (PromotionSku link : links) {
            Promotion p = link.getPromotion();
            eligible.computeIfAbsent(p.getId(), id ->
                (p.getUsageLimit() == null || usages.countByPromotionIdAndReleasedFalse(id) < p.getUsageLimit()) &&
                (p.getUsagePerCustomer() == null || usages.countByPromotionIdAndUserIdAndReleasedFalse(id, request.userId()) < p.getUsagePerCustomer()));
        }
        var candidates = links.stream().collect(Collectors.groupingBy(a -> a.getProductSku().getId(),
            Collectors.mapping(PromotionSku::getPromotion, Collectors.toList())));
        BigDecimal subtotal = skuList.stream().map(s -> PromotionPricingService.money(s.getPrice())
            .multiply(BigDecimal.valueOf(quantities.get(s.getId())))).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CheckoutResult.Line> lines = new ArrayList<>();
        for (ProductSku sku : skuList) {
            var result = PromotionPricingService.select(sku, candidates.getOrDefault(sku.getId(), List.of()),
                now, subtotal, p -> eligible.getOrDefault(p.getId(), false));
            lines.add(new CheckoutResult.Line(sku.getId(), quantities.get(sku.getId()), result.originalPrice(),
                result.discountAmount(), result.finalPrice(), result.promotionId(), result.promotionName()));
        }
        lines.sort(Comparator.comparing(CheckoutResult.Line::productSkuId));
        BigDecimal total = lines.stream().map(l -> l.finalPrice().multiply(BigDecimal.valueOf(l.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CheckoutResult(request.orderId(), lock ? "RESERVED" : "QUOTE", lines, total);
    }

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public CheckoutResult confirm(Long orderId) {
        CheckoutReservation r = locked(orderId);
        if ("RELEASED".equals(r.getStatus())) throw new BusinessLogicException("Checkout was released");
        if ("CONFIRMED".equals(r.getStatus())) return read(r);
        CheckoutResult result = read(r);
        var inventory = lockInventory(result);
        for (var line : result.items()) {
            Inventory i = inventory.get(line.productSkuId());
            if (i == null || i.getReservedQuantity() < line.quantity() || i.getQuantity() < line.quantity())
                throw new BusinessLogicException("Inventory reservation is inconsistent");
            long old = i.getQuantity();
            i.setReservedQuantity(i.getReservedQuantity() - line.quantity());
            i.setQuantity(old - line.quantity()); i.setAvailableQuantity(i.getQuantity() - i.getReservedQuantity());
            movement(i, -line.quantity(), old, orderId, "ORDER_PLACED");
        }
        r.setStatus("CONFIRMED");
        return read(r);
    }

    @Transactional
    public void release(Long orderId) {
        CheckoutReservation r = locked(orderId);
        if ("RELEASED".equals(r.getStatus())) return;
        List<PromotionUsage> used = usages.findByOrderId(orderId);
        if (!used.isEmpty()) promotions.lockAll(used.stream().map(u -> u.getPromotion().getId()).sorted().toList());
        CheckoutResult result = read(r);
        var inventory = lockInventory(result);
        for (var line : result.items()) {
            Inventory i = inventory.get(line.productSkuId());
            if ("CONFIRMED".equals(r.getStatus())) {
                long old = i.getQuantity();
                i.setQuantity(Math.addExact(old, line.quantity()));
                movement(i, line.quantity(), old, orderId, "ORDER_CANCELLED");
            } else {
                i.setReservedQuantity(i.getReservedQuantity() - line.quantity());
            }
            i.setAvailableQuantity(i.getQuantity() - i.getReservedQuantity());
        }
        used.forEach(u -> u.setReleased(true));
        r.setStatus("RELEASED");
    }

    private Map<Long, Inventory> lockInventory(CheckoutResult result) {
        return inventories.lockForCheckout(result.items().stream().map(CheckoutResult.Line::productSkuId).sorted().toList())
            .stream().collect(Collectors.toMap(i -> i.getProductSku().getId(), i -> i));
    }
    private void movement(Inventory i, long change, long old, Long orderId, String type) {
        movements.save(StockMovement.builder().inventory(i).quantityChange(change).previousQuantity(old)
            .newQuantity(i.getQuantity()).movementType(type).remark("Order " + orderId)
            .performedBy("order-service").warehouseLocation(i.getWarehouseLocation()).build());
    }
    private CheckoutReservation locked(Long id) {
        return reservations.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Checkout not found"));
    }
    private CheckoutResult read(CheckoutReservation r) {
        try {
            CheckoutResult saved = json.readValue(r.getSnapshot(), CheckoutResult.class);
            return new CheckoutResult(saved.orderId(), r.getStatus(), saved.items(), saved.total());
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read checkout snapshot", ex); }
    }
    private String write(CheckoutResult r) {
        try { return json.writeValueAsString(r); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot save checkout snapshot", ex); }
    }
    private static Map<Long, Long> quantities(CheckoutRequest r) {
        Map<Long, Long> result = new TreeMap<>();
        r.items().forEach(l -> result.merge(l.productSkuId(), l.quantity(), Math::addExact));
        return result;
    }
    private static Map<Long, Long> quantities(CheckoutResult r) {
        return r.items().stream().collect(Collectors.toMap(CheckoutResult.Line::productSkuId, CheckoutResult.Line::quantity));
    }
    private static void validate(CheckoutRequest r) {
        if (r == null || r.orderId() == null || r.orderId() < 1 || r.userId() == null || r.userId() < 1 ||
                r.items() == null || r.items().isEmpty() || r.items().size() > 100)
            throw new BusinessLogicException("Checkout requires an order, customer, and 1..100 items");
        for (var l : r.items()) if (l == null || l.productSkuId() == null || l.productSkuId() < 1 ||
                l.quantity() == null || l.quantity() < 1 || l.quantity() > 1000000)
            throw new BusinessLogicException("Invalid checkout SKU or quantity");
    }
}

