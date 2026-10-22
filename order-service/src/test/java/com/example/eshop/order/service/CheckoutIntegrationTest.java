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
        CartServiceImpl.class, CartMapper.class, CartItemMapper.class, AiShoppingService.class})
    static class Config {
        @Bean com.example.eshop.order.util.OrderCodeGenerator orderCodes(org.springframework.jdbc.core.JdbcTemplate jdbc, PlatformTransactionManager manager) {
            jdbc.execute("CREATE TABLE IF NOT EXISTS business_code_counters(code_type VARCHAR(30) PRIMARY KEY,last_value BIGINT NOT NULL)");
            jdbc.update("INSERT INTO business_code_counters(code_type,last_value) SELECT 'ORDER',0 WHERE NOT EXISTS(SELECT 1 FROM business_code_counters WHERE code_type='ORDER')");
            return new com.example.eshop.order.util.OrderCodeGenerator(jdbc,manager);
        }
        @Bean com.fasterxml.jackson.databind.ObjectMapper mapper() { return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(); }
        @Bean OrderService orderService() { return mock(OrderService.class); }
        @Bean com.example.eshop.order.automation.CheckoutAddressVerifier verifier() { return mock(com.example.eshop.order.automation.CheckoutAddressVerifier.class); }
        @Bean org.springframework.data.redis.core.StringRedisTemplate redis() { return mock(org.springframework.data.redis.core.StringRedisTemplate.class); }
        @Bean TransactionTemplate transactions(PlatformTransactionManager m) { return new TransactionTemplate(m); }
        @Bean CatalogAccess catalog() { return mock(CatalogAccess.class); }
        @Bean com.example.eshop.common.automation.AutomationEvents automation() { return mock(com.example.eshop.common.automation.AutomationEvents.class); }
    }
    @Autowired AiShoppingService shopping;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
    @Autowired com.example.eshop.order.automation.CheckoutAddressVerifier verifier;
    @Autowired OrderService orderService;
    @Autowired CheckoutCoordinator checkout;
    @Autowired CartServiceImpl carts;
    @Autowired CartRepository cartRepository;
    @Autowired OrderRepository orders;
    @Autowired CatalogAccess catalog;
    @Autowired com.example.eshop.common.automation.AutomationEvents automation;
    @Autowired TransactionTemplate tx;
    private static long sequence=100;
    private long user;

    @BeforeEach void setup() {
        reset(catalog,automation,redis,verifier,orderService); user=++sequence;
        var values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var reviews = new java.util.HashMap<String,String>();
        doAnswer(invocation -> { reviews.put(invocation.getArgument(0), invocation.getArgument(1)); return null; }).when(values).set(anyString(),anyString(),any(java.time.Duration.class));
        when(values.get(anyString())).thenAnswer(invocation -> reviews.get(invocation.getArgument(0)));
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
        when(catalog.quote(anyLong(), org.mockito.ArgumentMatchers.<com.example.eshop.common.dto.CheckoutRequest.Line>anyList())).thenAnswer(invocation -> {
            java.util.List<com.example.eshop.common.dto.CheckoutRequest.Line> requested = invocation.getArgument(1);
            var lines = requested.stream().map(line -> new CheckoutResult.Line(line.productSkuId(),line.quantity(),new BigDecimal("100"),new BigDecimal("20"),new BigDecimal("80"),7L,"Sale")).toList();
            return new CheckoutResult(1L,"QUOTE",lines,lines.stream().map(line->line.finalPrice().multiply(BigDecimal.valueOf(line.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add));
        });
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
        assertThat(first.getOrderNumber()).matches("anOrder_[0-9]{3,}");
        assertThat(first.getTotalAmount()).isEqualByComparingTo("160");
        assertThat(first.getItems().get(0).getBaseUnitPrice()).isEqualByComparingTo("100");
        assertThat(first.getItems().get(0).getFinalUnitPrice()).isEqualByComparingTo("80");
        assertThat(first.getItems().get(0).getPromotionName()).isEqualTo("Sale");
        OrderResponse repeated=(OrderResponse)checkout.create(user,request()).object();
        assertThat(repeated.getId()).isEqualTo(first.getId());
        assertThat(repeated.getOrderNumber()).isEqualTo(first.getOrderNumber());
        verify(catalog,times(1)).reserve(any()); verify(catalog,times(1)).confirm(first.getId());
        assertThat(cartRepository.findByUserId(user).orElseThrow().getTotalItems()).isZero();
    }
    @Test void cartReviewUsesOneBasketQuoteAndReplacesStaleStoredPrices() {
        tx.executeWithoutResult(t -> {
            var cart = cartRepository.lockByUserId(user).orElseThrow();
            cart.getCartItems().add(CartItemMapper.toEntity(cart,2L,1L,new BigDecimal("999")));
        });
        when(catalog.quote(anyLong(), org.mockito.ArgumentMatchers.<com.example.eshop.common.dto.CheckoutRequest.Line>anyList())).thenReturn(
                new CheckoutResult(1L,"QUOTE",List.of(
                        new CheckoutResult.Line(1L,2L,new BigDecimal("100"),new BigDecimal("70"),new BigDecimal("30"),7L,"Basket promotion"),
                        new CheckoutResult.Line(2L,1L,new BigDecimal("50"),BigDecimal.ZERO,new BigDecimal("50"),null,null)),new BigDecimal("110")));
        var reviewed = (CartResponse)carts.getCartByUserId(user).object();
        assertThat(reviewed.getTotalPrice()).isEqualByComparingTo("110");
        verify(catalog).quote(eq(user),org.mockito.ArgumentMatchers.<java.util.List<com.example.eshop.common.dto.CheckoutRequest.Line>>argThat(lines -> lines.size()==2));
        verify(catalog,never()).quote(anyLong(),anyLong(),anyLong());
    }
    @Test void deliveryAddressOwnershipIsRequiredBeforeAnyOrderIsCreated() {
        doThrow(new com.example.eshop.common.exception.BusinessLogicException("Delivery address does not belong to the customer"))
                .when(verifier).require(user,1L);
        assertThatThrownBy(() -> checkout.create(user,request())).hasMessageContaining("does not belong");
        verify(catalog,never()).reserve(any());
        assertThat(orders.findByUserIdAndCheckoutKey(user,request().getCheckoutKey())).isEmpty();
    }
    @Test void checkoutReviewIsOwnedAndExpiresWithoutAnOrderSideEffect() {
        var preview = shopping.preview(1L,"CASH_ON_DELIVERY");
        UUID id = UUID.fromString(preview.path("reviewId").asText());
        assertThat(shopping.review(id).path("cart").path("total_price").decimalValue()).isEqualByComparingTo("160");
        verify(verifier).require(user,1L);
        var other = new UsernamePasswordAuthenticationToken("other",null,List.of()); other.setDetails(Map.of("userId",user+1000));
        SecurityContextHolder.getContext().setAuthentication(other);
        assertThatThrownBy(() -> shopping.review(id)).hasMessageContaining("expired or unavailable");
        verifyNoInteractions(orderService);
    }
    @Test void changedPriceRequiresANewCheckoutReviewBeforeOrderCreation() {
        var preview = shopping.preview(1L,"CASH_ON_DELIVERY");
        when(catalog.quote(anyLong(), org.mockito.ArgumentMatchers.<com.example.eshop.common.dto.CheckoutRequest.Line>anyList())).thenReturn(new CheckoutResult(1L,"QUOTED",List.of(
                new CheckoutResult.Line(1L,2L,new BigDecimal("100"),BigDecimal.TEN,new BigDecimal("90"),null,null)),new BigDecimal("180")));
        UUID id = UUID.fromString(preview.path("reviewId").asText());
        assertThatThrownBy(() -> shopping.create(id)).hasMessageContaining("Cart or price changed");
        verifyNoInteractions(orderService);
    }
    @Test void cartWriteReceiptsPreventDuplicateAdditionAndConflictingKeyReuse() {
        UUID key = UUID.randomUUID();
        var first = shopping.mutate(key,"add",1L,null,1L);
        var second = shopping.mutate(key,"add",1L,null,1L);
        assertThat(second.toString()).isEqualTo(first.toString());
        assertThat(second.path("items").get(0).path("quantity").asLong()).isEqualTo(3);
        verify(catalog,times(1)).quote(user,1L,3L);
        assertThatThrownBy(() -> shopping.mutate(key,"add",1L,null,2L)).hasMessageContaining("another cart operation");
        assertThat(((CartResponse)carts.getCartByUserId(user).object()).getItems().get(0).getQuantity()).isEqualTo(3);
    }
    @Test void anotherCustomersCartItemCannotBeUpdated() {
        long itemId = ((CartResponse)carts.getCartByUserId(user).object()).getItems().get(0).getId();
        var auth = new UsernamePasswordAuthenticationToken("other", null, List.of()); auth.setDetails(Map.of("userId", user + 1000));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(() -> shopping.mutate(UUID.randomUUID(),"update",1L,itemId,3L)).hasMessageContaining("Cart item unavailable");
        verify(catalog,never()).quote(anyLong(),anyLong(),anyLong());
    }
    @Test void changedConfirmedPriceReleasesReservationAndNeverEmitsPaymentEvent() {
        var request = request(); request.setExpectedTotal(new BigDecimal("100"));
        var result = (OrderResponse)checkout.create(user, request).object();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(catalog).release(result.getId()); verify(catalog,never()).confirm(anyLong());
        verifyNoInteractions(automation);
        assertThat(cartRepository.findByUserId(user).orElseThrow().getCheckoutOrderId()).isNull();
    }
    @Test void changedCartFingerprintCannotCreateAnOrder() {
        var request = request(); request.setExpectedCartFingerprint("0".repeat(64));
        assertThatThrownBy(() -> checkout.create(user, request)).hasMessageContaining("Cart changed");
        verify(catalog,never()).reserve(any());
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
    @Test void onlineCheckoutReservesWithoutConfirmingStockAndEmitsOnce() {
        var request=request();request.setPaymentMethod("BAKONG");
        var first=(OrderResponse)checkout.create(user,request).object();
        checkout.create(user,request);
        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(first.getTotalAmount()).isEqualByComparingTo("160");
        verify(catalog,never()).confirm(anyLong());
        verify(catalog,times(1)).reserve(any());
        tx.executeWithoutResult(t -> verify(automation,times(1)).emit(eq(com.example.eshop.common.automation.CommerceEvent.ORDERS),argThat(e->e.type()==com.example.eshop.common.automation.CommerceEvent.Type.ORDER_CREATED)));
        tx.executeWithoutResult(t->assertThat(orders.findById(first.getId()).orElseThrow().isCatalogCompleted()).isFalse());
    }
}
