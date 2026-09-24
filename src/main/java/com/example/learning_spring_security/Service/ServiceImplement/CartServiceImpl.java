package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.Service.ServiceStructure.CartService;
import com.example.learning_spring_security.ServiceMapper.CartItemMapper;
import com.example.learning_spring_security.ServiceMapper.CartMapper;
import com.example.learning_spring_security.dto.Request.CartRequest;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCartByUserId(Long userId) {
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

           ProductSku productSku = productSkuRepository.findById(request.getProductSkuId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product SKU not found: " + request.getProductSkuId()));

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item ->
                        item.getProductSku().getId().equals(productSku.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();

            long quantity = item.getQuantity() + request.getQuantity();

            item.setQuantity(quantity);
            item.setTotalPrice(
                    productSku.getPrice().multiply(BigDecimal.valueOf(quantity)));
        } else {

            CartItem newItem =
                    CartItemMapper.toEntity(cart, productSku, request.getQuantity());

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

            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(cartItem.getProductSku().getPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity())));
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
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .totalPrice(BigDecimal.ZERO)
                            .totalItems(0)

                            .build();

                    return cartRepository.save(newCart);
                });
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