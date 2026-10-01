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
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CatalogAccess catalog;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCartByUserId(Long userId) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
        return cartMapper.toResponse(cart);
    }

    @Override
    public ResponseErrorTemplate addItemToCart(Long userId, CartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);
        if (cart.getCartItems() == null) {
            cart.setCartItems(new ArrayList<>());
        }

        // Catalog is authoritative; legacy unit_price input is ignored.
        BigDecimal unitPrice = catalog.quote(userId, request.getProductSkuId(), request.getQuantity()).items().get(0).finalPrice();

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductSkuId().equals(request.getProductSkuId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();

            long quantity = item.getQuantity() + request.getQuantity();

            item.setQuantity(quantity);
            item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        } else {
            CartItem newItem = CartItemMapper.toEntity(cart, request.getProductSkuId(), request.getQuantity(), unitPrice);
            cart.getCartItems().add(newItem);
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public ResponseErrorTemplate updateCartItem(Long userId, Long cartItemId, CartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (request.getQuantity() <= 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            BigDecimal unitPrice = catalog.quote(userId, cartItem.getProductSkuId(), request.getQuantity()).items().get(0).finalPrice();
            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())));
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public ResponseErrorTemplate removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public ResponseErrorTemplate clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);

        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();

        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
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
        if (cart.getCheckoutOrderId() != null)
            throw new com.example.eshop.common.exception.BusinessLogicException("Checkout is processing; retry after it finishes");
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
