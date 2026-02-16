package com.example.learning_spring_security.Service.implement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.Model.CartItem;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.CartItemRepository;
import com.example.learning_spring_security.Repository.CartRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.CartService;
import com.example.learning_spring_security.ServiceMapper.CartItemMapper;
import com.example.learning_spring_security.ServiceMapper.CartMapper;
import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.CartResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
        return CartMapper.toResponse(cart);
    }

    @Override
    public CartResponse addItemToCart(Long userId, CartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        ProductSku productSku = productSkuRepository.findById(request.getProductSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found with id: " + request.getProductSkuId()));

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductSku().getId().equals(request.getProductSkuId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            Long newQuantity = item.getQuantity() + request.getQuantity();
            item.setQuantity(newQuantity);
            item.setTotalPrice(productSku.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // Create new cart item
            CartItem newItem = CartItemMapper.toEntity(cart, productSku, request.getQuantity());
            cart.getCartItems().add(newItem);
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return CartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse updateCartItem(Long userId, Long cartItemId, CartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (request.getQuantity() <= 0) {
            // Remove item if quantity is 0 or negative
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            // Update quantity
            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(cartItem.getProductSku().getPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity())));
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return CartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return CartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);

        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getCartItems().clear();

        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setTotalItems(0);

        Cart savedCart = cartRepository.save(cart);
        return CartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse getOrCreateCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return CartMapper.toResponse(cart);
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