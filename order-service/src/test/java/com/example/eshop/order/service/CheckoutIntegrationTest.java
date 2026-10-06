package com.example.eshop.order.service;

import com.example.eshop.order.model.*;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.mapper.*;
import com.example.eshop.order.service.impl.*;
import com.example.eshop.order.dto.request.*;
import com.example.eshop.order.dto.response.*;
import com.example.eshop.common.dto.CheckoutResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:checkout;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CheckoutIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CheckoutIntegrationTest {
    @Configuration @EntityScan("com.example.eshop.order.model")
    @EnableJpaRepositories("com.example.eshop.order.repository")
    @Import({CheckoutCoordinator.class, OrderMapper.class, OrderItemMapper.class,
        CartServiceImpl.class, CartMapper.class, CartItemMapper.class})
    static class Config {
        @Bean TransactionTemplate transactions(PlatformTransactionManager m) { return new TransactionTemplate(m); }
        @Bean CatalogAccess catalog() { return mock(CatalogAccess.class); }
    }
    @Autowired CheckoutCoordinator checkout;
    @Autowired CartServiceImpl carts;
    @Autowired CartRepository cartRepository;
    @Autowired OrderRepository orders;
    @Autowired CatalogAccess catalog;
    @Autowired TransactionTemplate tx;
    private static long sequence=100;
    private long user;

    @BeforeEach void setup() {
        reset(catalog); user=++sequence;
        var auth = new UsernamePasswordAuthenticationToken("customer", null, List.of());
        auth.setDetails(Map.of("userId",user));
        SecurityContextHolder.getContext().setAuthentication(auth);
        tx.executeWithoutResult(t -> {
            Cart c=new Cart(); c.setUserId(user); c.setTotalPrice(new BigDecimal("0.02")); c.setTotalItems(2);
            c.getCartItems().add(CartItemMapper.toEntity(c,1L,2L,new BigDecimal("0.01")));
            cartRepository.save(c);
        });
        when(catalog.reserve(any())).thenAnswer(invocation -> result(((com.example.eshop.common.dto.CheckoutRequest)invocation.getArgument(0)).orderId()));
        when(catalog.confirm(anyLong())).thenAnswer(invocation -> result(invocation.getArgument(0)));
        when(catalog.quote(anyLong(), anyLong(), anyLong())).thenReturn(result(1L));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private CheckoutResult result(Long id) {
        return new CheckoutResult(id,"CONFIRMED",List.of(new CheckoutResult.Line(1L,2L,
            new BigDecimal("100"),new BigDecimal("20"),new BigDecimal("80"),7L,"Sale")),new BigDecimal("160"));
    }
    private OrderRequest request() { return OrderRequest.builder().addressId(1L).paymentMethod("CASH_ON_DELIVERY")
        .currency("USD").checkoutKey("key-"+user).build(); }

    @Test void checkoutIgnoresCartPricePersistsSnapshotsAndRetriesOnce() {
        OrderResponse first=(OrderResponse)checkout.create(user,request()).object();
        assertThat(first.getTotalAmount()).isEqualByComparingTo("160");
        assertThat(first.getItems().get(0).getBaseUnitPrice()).isEqualByComparingTo("100");
        assertThat(first.getItems().get(0).getFinalUnitPrice()).isEqualByComparingTo("80");
        assertThat(first.getItems().get(0).getPromotionName()).isEqualTo("Sale");
        OrderResponse repeated=(OrderResponse)checkout.create(user,request()).object();
        assertThat(repeated.getId()).isEqualTo(first.getId());
        verify(catalog,times(1)).reserve(any()); verify(catalog,times(1)).confirm(first.getId());
        assertThat(cartRepository.findByUserId(user).orElseThrow().getTotalItems()).isZero();
    }
    @Test void interruptionKeepsCartLockedAndRecoveryFinishesSameOrder() {
        when(catalog.confirm(anyLong())).thenThrow(new IllegalStateException("network unavailable"))
            .thenAnswer(invocation -> result(invocation.getArgument(0)));
        OrderResponse pending=(OrderResponse)checkout.create(user,request()).object();
        assertThat(pending.getStatus()).isEqualTo("CHECKOUT_PENDING");
        assertThat(cartRepository.findByUserId(user).orElseThrow().getCheckoutOrderId()).isEqualTo(pending.getId());
        checkout.process(pending.getId());
        assertThat(orders.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        verify(catalog,times(2)).reserve(any());
    }
    @Test void cartRejectsCustomerSpoofingAndIgnoresSubmittedUnitPrice() {
        CartRequest request=CartRequest.builder().productSkuId(1L).quantity(1L).unitPrice(new BigDecimal("0.01")).build();
        CartResponse cart=(CartResponse)carts.addItemToCart(user,request).object();
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("240");
        assertThatThrownBy(() -> carts.addItemToCart(user+1,request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void completedCheckoutUnlocksStaleCartWithoutRemovingItems() {
        OrderResponse placed=(OrderResponse)checkout.create(user,request()).object();
        tx.executeWithoutResult(t -> {
            OrderDetail order=orders.findById(placed.getId()).orElseThrow();
            order.setStatus("DELIVERED");
            Cart cart=cartRepository.findByUserId(user).orElseThrow();
            cart.setCheckoutOrderId(placed.getId());
            cart.getCartItems().add(CartItemMapper.toEntity(cart,1L,1L,new BigDecimal("80")));
            cart.setTotalItems(1); cart.setTotalPrice(new BigDecimal("80"));
        });
        CartResponse recovered=(CartResponse)carts.getOrCreateCart(user).object();
        assertThat(recovered.getTotalItems()).isEqualTo(1);
        assertThat(cartRepository.findByUserId(user).orElseThrow().getCheckoutOrderId()).isNull();
        assertThat(orders.findById(placed.getId()).orElseThrow().getStatus()).isEqualTo("DELIVERED");
    }
    @Test void activeCheckoutRemainsLockedForCartChanges() {
        when(catalog.confirm(anyLong())).thenThrow(new IllegalStateException("network unavailable"));
        OrderResponse pending=(OrderResponse)checkout.create(user,request()).object();
        assertThatThrownBy(() -> carts.getOrCreateCart(user))
            .isInstanceOf(com.example.eshop.common.exception.BusinessLogicException.class);
        assertThat(cartRepository.findByUserId(user).orElseThrow().getCheckoutOrderId()).isEqualTo(pending.getId());
    }
    @Test void cancelledOrderReleasesCatalogExactlyOnce() {
        OrderResponse order=(OrderResponse)checkout.create(user,request()).object();
        tx.executeWithoutResult(t -> {
            OrderDetail entity=orders.findById(order.getId()).orElseThrow();
            entity.setStatus("CANCELLED"); entity.setCatalogCompleted(false);
        });
        checkout.process(order.getId()); checkout.process(order.getId());
        verify(catalog,times(1)).release(order.getId());
    }
}

