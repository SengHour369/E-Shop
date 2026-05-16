package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductSkuRepository productSkuRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private User user;
    private Product product;
    private ProductSku productSku;
    private CartItem cartItem;
    private CartRequest cartRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").build();
        product = Product.builder().id(1L).name("Test Product").build();
        productSku = ProductSku.builder()
                .id(1L)
                .product(product)
                .price(BigDecimal.valueOf(100))
                .build();
        cart = Cart.builder()
                .id(1L)
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .totalItems(0)
                .cartItems(new ArrayList<>())
                .build();
        cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .productSku(productSku)
                .quantity(2L)
                .totalPrice(BigDecimal.valueOf(200))
                .build();
        cartRequest = CartRequest.builder()
                .productId(1L)
                .quantity(1L)
                .build();
    }

    @Test
    void getCartByUserId_ShouldReturnCart_WhenExists() {
        // Given
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        // When
        ResponseErrorTemplate response = cartService.getCartByUserId(1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).findByUserIdWithItems(1L);
    }

    @Test
    void getCartByUserId_ShouldThrowException_WhenNotFound() {
        // Given
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartService.getCartByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found for user id: 1");
    }

    @Test
    void addItemToCart_ShouldAddNewItem_WhenItemDoesNotExist() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(productSku));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.addItemToCart(1L, cartRequest);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_ShouldUpdateExistingItem_WhenItemExists() {
        // Given
        cart.getCartItems().add(cartItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(productSku));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.addItemToCart(1L, cartRequest);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartService.addItemToCart(1L, cartRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found for product id: 1");
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenProductSkuNotFound() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productSkuRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartService.addItemToCart(1L, cartRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product sku not found for product id: 1");
    }

    @Test
    void updateCartItem_ShouldUpdateQuantity() {
        // Given
        cart.getCartItems().add(cartItem);
        CartRequest updateRequest = CartRequest.builder().quantity(3L).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.updateCartItem(1L, 1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    void updateCartItem_ShouldRemoveItem_WhenQuantityIsZero() {
        // Given
        cart.getCartItems().add(cartItem);
        CartRequest updateRequest = CartRequest.builder().quantity(0L).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.updateCartItem(1L, 1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(cartItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateCartItem_ShouldThrowException_WhenCartItemNotFound() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // When & Then
        assertThatThrownBy(() -> cartService.updateCartItem(1L, 1L, cartRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found with id: 1");
    }

    @Test
    void removeItemFromCart_ShouldRemoveItem() {
        // Given
        cart.getCartItems().add(cartItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.removeItemFromCart(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(cartItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItemFromCart_ShouldThrowException_WhenCartItemNotFound() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // When & Then
        assertThatThrownBy(() -> cartService.removeItemFromCart(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found with id: 1");
    }

    @Test
    void clearCart_ShouldClearAllItems() {
        // Given
        cart.getCartItems().add(cartItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.clearCart(1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).deleteAllByCartId(1L);
        verify(cartRepository).save(cart);
    }

    @Test
    void getOrCreateCart_ShouldReturnExistingCart() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // When
        ResponseErrorTemplate response = cartService.getOrCreateCart(1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).findByUserId(1L);
    }

    @Test
    void getOrCreateCart_ShouldCreateNewCart_WhenNotExists() {
        // Given
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        ResponseErrorTemplate response = cartService.getOrCreateCart(1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }
}
