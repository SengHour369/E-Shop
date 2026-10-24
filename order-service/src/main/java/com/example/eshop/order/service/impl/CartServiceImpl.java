package com.example.eshop.order.service.impl;

import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.order.model.Cart;
import com.example.eshop.order.model.CartItem;
import com.example.eshop.order.repository.CartItemRepository;
import com.example.eshop.order.repository.CartRepository;
import com.example.eshop.order.service.CartService;
import com.example.eshop.order.mapper.CartItemMapper;
import com.example.eshop.order.mapper.CartMapper;
import com.example.eshop.order.dto.request.CartRequest;

import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final com.example.eshop.order.repository.OrderRepository orders;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CatalogAccess catalog;

    @Override
    public ResponseErrorTemplate getCartByUserId(Long userId) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        Cart cart = cartRepository.lockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found"
                ));
        return snapshot(cart);
    }

    private ResponseErrorTemplate snapshot(Cart cart) {
        if (cart.getCheckoutOrderId() == null
                && !cart.getCartItems().isEmpty()) {
            // Basket-wide promotion thresholds must see all items, exactly as checkout does.
            var quote = quote(
                    cart,
                    cart.getCartItems().stream()
                            .map(item ->
                                    new com.example.eshop.common.dto.CheckoutRequest.Line(
                                            item.getProductSkuId(),
                                            item.getQuantity()
                                    )
                            )
                            .toList()
            );
            var prices = quote.items().stream().collect(
                    java.util.stream.Collectors.toMap(
                            com.example.eshop.common.dto.CheckoutResult.Line::productSkuId,
                            java.util.function.Function.identity()
                    )
            );

            for (var item : cart.getCartItems()) {
                var line = prices.get(item.getProductSkuId());

                if (line == null
                        || !item.getQuantity().equals(line.quantity())
                        || line.finalPrice() == null
                        || line.finalPrice().signum() < 0) {
                    throw new com.example.eshop.common.exception.BusinessLogicException(
                            "Catalog quote does not match the cart"
                    );
                }

                item.setTotalPrice(
                        line.finalPrice().multiply(
                                BigDecimal.valueOf(item.getQuantity())
                        )
                );
            }

            updateCartTotals(cart);
        }

        return cartMapper.toResponse(cart);
    }

    @Override
    public ResponseErrorTemplate addItemToCart(
            Long userId,
            CartRequest request
    ) {
        return add(getOrCreateCartEntity(userId), request);
    }

    private ResponseErrorTemplate add(Cart cart, CartRequest request) {
        if (cart.getCartItems() == null) {
            cart.setCartItems(new ArrayList<>());
        }

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item ->
                        item.getProductSkuId().equals(
                                request.getProductSkuId()
                        )
                )
                .findFirst();
        long requestedQuantity = Math.addExact(
                existingItem.map(CartItem::getQuantity).orElse(0L),
                request.getQuantity()
        );

        if (requestedQuantity > 999) {
            throw new com.example.eshop.common.exception.BusinessLogicException(
                    "Cart quantity must not exceed 999"
            );
        }

        BigDecimal unitPrice = unitQuote(
                cart,
                request.getProductSkuId(),
                requestedQuantity
        ).items().get(0).finalPrice();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();

            long quantity = item.getQuantity() + request.getQuantity();

            item.setQuantity(quantity);
            item.setTotalPrice(
                    unitPrice.multiply(BigDecimal.valueOf(quantity))
            );
        } else {
            CartItem newItem = CartItemMapper.toEntity(
                    cart,
                    request.getProductSkuId(),
                    request.getQuantity(),
                    unitPrice
            );
            cart.getCartItems().add(newItem);
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return savedCart.getGuestSessionId() == null
                ? cartMapper.toResponse(savedCart)
                : snapshot(savedCart);
    }

    @Override
    public ResponseErrorTemplate updateCartItem(
            Long userId,
            Long cartItemId,
            CartRequest request
    ) {
        return update(getOrCreateCartEntity(userId), cartItemId, request);
    }

    private ResponseErrorTemplate update(
            Cart cart,
            Long cartItemId,
            CartRequest request
    ) {
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId
                ));

        if (request.getQuantity() <= 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            BigDecimal unitPrice = unitQuote(
                    cart,
                    cartItem.getProductSkuId(),
                    request.getQuantity()
            ).items().get(0).finalPrice();
            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(
                    unitPrice.multiply(
                            BigDecimal.valueOf(request.getQuantity())
                    )
            );
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return savedCart.getGuestSessionId() == null
                ? cartMapper.toResponse(savedCart)
                : snapshot(savedCart);
    }

    @Override
    public ResponseErrorTemplate removeItemFromCart(
            Long userId,
            Long cartItemId
    ) {
        return remove(getOrCreateCartEntity(userId), cartItemId);
    }

    private ResponseErrorTemplate remove(Cart cart, Long cartItemId) {
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId
                ));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return savedCart.getGuestSessionId() == null
                ? cartMapper.toResponse(savedCart)
                : snapshot(savedCart);
    }

    @Override
    public ResponseErrorTemplate clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);

        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();

        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);

        Cart savedCart = cartRepository.save(cart);
        return savedCart.getGuestSessionId() == null
                ? cartMapper.toResponse(savedCart)
                : snapshot(savedCart);
    }

    @Override
    public ResponseErrorTemplate getOrCreateCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return cartMapper.toResponse(cart);
    }

    private Cart getOrCreateCartEntity(Long userId) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        Cart cart = cartRepository.lockByUserId(userId)
                .orElseGet(() -> {
                    // TODO: cross-service call via Feign to auth-service to verify the user exists.
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .totalPrice(BigDecimal.ZERO)
                            .totalItems(0)
                            .build();

                    newCart.setCartItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        if (cart.getCheckoutOrderId() != null) {
            var checkoutOrder = orders.findById(
                    cart.getCheckoutOrderId()
            ).orElse(null);
            boolean completed = checkoutOrder != null
                    && userId.equals(checkoutOrder.getUserId())
                    && checkoutOrder.isCatalogCompleted()
                    && java.util.Set.of(
                    "PENDING",
                    "CONFIRMED",
                    "PROCESSING",
                    "SHIPPED",
                    "DELIVERED",
                    "FAILED",
                    "CANCELLED",
                    "REFUNDED"
            ).contains(checkoutOrder.getStatus());

            if (!completed) {
                throw new com.example.eshop.common.exception.BusinessLogicException(
                        "Checkout is processing; retry after it finishes"
                );
            }

            // Recover a stale lock without removing items or changing the order.
            cart.setCheckoutOrderId(null);
        }

        return cart;
    }

    private com.example.eshop.common.dto.CheckoutResult unitQuote(
            Cart cart,
            Long skuId,
            Long quantity
    ) {
        return cart.getGuestSessionId() == null
                ? catalog.quote(cart.getUserId(), skuId, quantity)
                : catalog.guestQuote(
                cart.getGuestSessionId(),
                java.util.List.of(
                        new com.example.eshop.common.dto.CheckoutRequest.Line(
                                skuId,
                                quantity
                        )
                )
        );
    }

    private com.example.eshop.common.dto.CheckoutResult quote(
            Cart cart,
            java.util.List<com.example.eshop.common.dto.CheckoutRequest.Line> lines
    ) {
        return cart.getGuestSessionId() == null
                ? catalog.quote(cart.getUserId(), lines)
                : catalog.guestQuote(cart.getGuestSessionId(), lines);
    }

    // Guest entrypoints receive only a server-validated session ID, never a browser-provided owner.
    public ResponseErrorTemplate guestCart(java.util.UUID id) {
        return snapshot(guestEntity(id));
    }

    public ResponseErrorTemplate guestAdd(
            java.util.UUID id,
            CartRequest request
    ) {
        return add(guestEntity(id), request);
    }

    public ResponseErrorTemplate guestUpdate(
            java.util.UUID id,
            Long itemId,
            CartRequest request
    ) {
        return update(guestEntity(id), itemId, request);
    }

    public ResponseErrorTemplate guestClear(java.util.UUID id) {
        var cart = guestEntity(id);
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);
        return snapshot(cart);
    }

    public ResponseErrorTemplate guestRemove(
            java.util.UUID id,
            Long itemId
    ) {
        return remove(guestEntity(id), itemId);
    }

    private Cart guestEntity(java.util.UUID id) {
        Cart cart = cartRepository.findByGuestSessionId(id)
                .orElseGet(() -> {
                    Cart created = Cart.builder()
                            .guestSessionId(id)
                            .totalPrice(BigDecimal.ZERO)
                            .totalItems(0)
                            .build();
                    created.setCartItems(new ArrayList<>());
                    return cartRepository.saveAndFlush(created);
                });

        if (cart.getCheckoutOrderId() != null) {
            throw new com.example.eshop.common.exception.BusinessLogicException(
                    "Checkout is processing; wait for confirmation"
            );
        }

        return cart;
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal totalPrice = cart.getCartItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getCartItems().stream()
                .mapToInt(item -> item.getQuantity().intValue())
                .sum();

        cart.setTotalPrice(totalPrice);
        cart.setTotalItems(totalItems);
    }
}