package com.example.eshop.order.guest;

import com.example.eshop.order.model.*;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.mapper.*;
import com.example.eshop.order.service.impl.*;
import com.example.eshop.common.dto.*;
import com.example.eshop.common.storefront.*;
import com.example.eshop.common.automation.AutomationEvents;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.*;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql=false, properties={
    "spring.datasource.url=jdbc:h2:mem:guest-commerce;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes=GuestCommerceIntegrationTest.Config.class)
class GuestCommerceIntegrationTest {
    @Configuration @EntityScan({"com.example.eshop.order.model","com.example.eshop.order.guest"})
    @EnableJpaRepositories({"com.example.eshop.order.repository","com.example.eshop.order.guest"})
    @Import({GuestSessionService.class, GuestOrderService.class, CheckoutCoordinator.class,
        CartServiceImpl.class, CartMapper.class, CartItemMapper.class, OrderMapper.class, OrderItemMapper.class})
    static class Config {
        @Bean com.example.eshop.order.util.OrderCodeGenerator orderCodes(org.springframework.jdbc.core.JdbcTemplate jdbc, PlatformTransactionManager manager) {
            jdbc.execute("CREATE TABLE IF NOT EXISTS business_code_counters(code_type VARCHAR(30) PRIMARY KEY,last_value BIGINT NOT NULL)");
            jdbc.update("INSERT INTO business_code_counters(code_type,last_value) SELECT 'ORDER',0 WHERE NOT EXISTS(SELECT 1 FROM business_code_counters WHERE code_type='ORDER')");
            return new com.example.eshop.order.util.OrderCodeGenerator(jdbc,manager);
        }
        @Bean GuestSessionCookieService cookies() {return new GuestSessionCookieService(true,Set.of("https://shop.test"));}
        @Bean com.example.eshop.common.audit.AuditLogService audit() {return mock(com.example.eshop.common.audit.AuditLogService.class);}
        @Bean GuestRateLimiter limiter() {return mock(GuestRateLimiter.class);}
        @Bean CatalogAccess catalog() {return mock(CatalogAccess.class);}
        @Bean StorefrontAccessClient access() {return mock(StorefrontAccessClient.class);}
        @Bean com.example.eshop.order.automation.PaymentAutomationClient payments() {return mock(com.example.eshop.order.automation.PaymentAutomationClient.class);}
        @Bean AutomationEvents events() {return mock(AutomationEvents.class);}
        @Bean TransactionTemplate transactions(PlatformTransactionManager manager) {return new TransactionTemplate(manager);}
        @Bean com.fasterxml.jackson.databind.ObjectMapper mapper() {return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();}
    }
    @Autowired GuestSessionService sessions;
    @Autowired GuestSessionRepository sessionRepository;
    @Autowired GuestOrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired CartServiceImpl carts;
    @Autowired CartRepository cartRepository;
    @Autowired CatalogAccess catalog;
    @Autowired StorefrontAccessClient access;
    @Autowired AutomationEvents events;
    @Autowired com.example.eshop.order.automation.PaymentAutomationClient paymentClient;
    private MockHttpServletRequest request;
    private GuestSession session;
    private String token;
    private GuestCheckoutRequest checkout;

    @BeforeEach void setup() {
        reset(catalog,access,events);
        when(access.current()).thenReturn(enabled());
        doCallRealMethod().when(access).require(anyBoolean());
        when(catalog.guestQuote(any(),anyList())).thenAnswer(i->quote(1L,i.getArgument(1)));
        when(catalog.reserve(any())).thenAnswer(i->{CheckoutRequest r=i.getArgument(0);return quote(r.orderId(),r.items());});
        when(catalog.confirm(anyLong())).thenAnswer(i->quote(i.getArgument(0),List.of(new CheckoutRequest.Line(5L,2L))));
        var start=new MockHttpServletRequest();start.addHeader("Origin","https://shop.test");
        var response=new MockHttpServletResponse();var view=sessions.establish(start,response);
        token=response.getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1];
        request=new MockHttpServletRequest();request.addHeader("Origin","https://shop.test");
        request.setCookies(new Cookie(GuestSessionCookieService.COOKIE,token));request.addHeader("X-Guest-CSRF",view.csrfToken());
        session=sessions.require(request,true);
        checkout=new GuestCheckoutRequest(new GuestDelivery("Guest Buyer","012345678","buyer@example.test","Phnom Penh","District","Commune","Village","House 1",""),
                "checkout-key-123456789","CASH_ON_DELIVERY",new BigDecimal("20.00"));
    }
    private StorefrontAccess enabled() {return new StorefrontAccess(true,true,true,true,true,true,false,true);}
    private CheckoutResult quote(Long id,List<CheckoutRequest.Line> items) {
        var lines=items.stream().map(l->new CheckoutResult.Line(l.productSkuId(),l.quantity(),BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.TEN,null,null)).toList();
        return new CheckoutResult(id,"RESERVED",lines,lines.stream().map(l->l.finalPrice().multiply(BigDecimal.valueOf(l.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    private void seedCart() {
        var body=com.example.eshop.order.dto.request.CartRequest.builder().productSkuId(5L).quantity(2L).unitPrice(BigDecimal.ONE).build();
        carts.guestAdd(session.getId(),body);
    }
    @Test void cookieIsOpaqueSecureAndOnlyItsHashIsStored() {
        assertThat(token).matches("[A-Za-z0-9_-]{43}");assertThat(session.getTokenHash()).isNotEqualTo(token).hasSize(64);
        var start=new MockHttpServletRequest();start.addHeader("Origin","https://shop.test");var response=new MockHttpServletResponse();
        sessions.establish(start,response);String header=response.getHeader("Set-Cookie");
        assertThat(header).contains("HttpOnly","Secure","SameSite=Lax","Path=/api/v1/guest").doesNotContain("buyer@example.test");
    }
    @Test void csrfOriginAndExpiredSessionFailClosed() {
        request.removeHeader("X-Guest-CSRF");assertThatThrownBy(()->sessions.require(request,true)).hasMessageContaining("403");
        request.addHeader("Origin","https://attacker.test");assertThatThrownBy(()->sessions.establish(request,new MockHttpServletResponse())).hasMessageContaining("403");
        session.setExpiresAt(Instant.now().minusSeconds(1));sessionRepository.saveAndFlush(session);
        assertThatThrownBy(()->sessions.require(request,false)).hasMessageContaining("401");
    }
    @Test void bootstrapRotatesTokenWithoutChangingCartOwnership() {
        var response=new MockHttpServletResponse();sessions.establish(request,response);
        assertThat(response.getHeader("Set-Cookie")).doesNotContain("eshop_guest="+token+";");
        assertThatThrownBy(()->sessions.require(request,false)).hasMessageContaining("401");
    }
    @Test void guestCartsAreIsolatedAndNeverTrustFrontendPrices() {
        seedCart();var cart=cartRepository.findByGuestSessionId(session.getId()).orElseThrow();
        assertThat(cart.getUserId()).isNull();assertThat(cart.getTotalPrice()).isEqualByComparingTo("20.00");
        var another=new GuestSession();another.setTokenHash("a".repeat(64));another.setCsrfHash("b".repeat(64));another.setExpiresAt(Instant.now().plusSeconds(60));sessionRepository.saveAndFlush(another);
        var item=cart.getCartItems().get(0);
        assertThatThrownBy(()->carts.guestRemove(another.getId(),item.getId())).isInstanceOf(com.example.eshop.common.exception.ResourceNotFoundException.class);
        assertThat(cart.getCartItems()).hasSize(1);
    }
    @Test void checkoutIsIdempotentAndSnapshotIsNotAnIdentityAddress() {
        seedCart();var first=orders.create(session,checkout);var second=orders.create(session,checkout);
        assertThat(first.order().get("id")).isEqualTo(second.order().get("id"));
        var order=orderRepository.findById((Long)first.order().get("id")).orElseThrow();
        assertThat(order.getUserId()).isNull();assertThat(order.getGuestSessionId()).isEqualTo(session.getId());
        assertThat(order.getShippingAddressId()).isNull();assertThat(order.getShippingAddressSnapshot()).contains("House 1");
        assertThat(order.getStatus()).isEqualTo("PENDING");verify(catalog,times(1)).reserve(any());verify(catalog,times(1)).confirm(anyLong());
        assertThat(first.order().toString()).doesNotContain("buyer@example.test","012345678","House 1");
    }
    @Test void unsafeSettingsAndUnverifiedContactBlockCheckout() {
        seedCart();when(access.current()).thenReturn(StorefrontAccess.defaults());
        assertThatThrownBy(()->orders.create(session,checkout)).hasMessageContaining("403");
        when(access.current()).thenReturn(new StorefrontAccess(true,false,true,true,false,true,true,true));
        assertThatThrownBy(()->orders.create(session,checkout)).hasMessageContaining("428");verify(catalog,never()).reserve(any());
    }
    @Test void priceChangeFailsWithoutPaymentAndReleasesReservation() {
        seedCart();var changed=new GuestCheckoutRequest(checkout.delivery(),checkout.idempotencyKey(),"BAKONG",BigDecimal.ONE);
        var confirmation=orders.create(session,changed);
        assertThat(confirmation.order().get("status")).isEqualTo("FAILED");verify(catalog).release(anyLong());verify(events,never()).emit(any(),any());
    }
    @Test void ownershipAndScopedTokensBothProtectOrderDetails() {
        seedCart();var confirmation=orders.create(session,checkout);long id=(Long)confirmation.order().get("id");
        assertThat(orders.detail(session,id,confirmation.orderAccessToken())).containsEntry("customerType","GUEST");
        assertThatThrownBy(()->orders.detail(session,id,"x".repeat(43))).hasMessageContaining("404");
        var other=new GuestSession();assertThatThrownBy(()->orders.detail(other,id,confirmation.orderAccessToken())).hasMessageContaining("404");
    }
    @Test void deliveryValidationRejectsIncompletePhysicalAddresses() {
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var bad=new GuestDelivery("","not-phone","bad","","","","","","");
            assertThat(factory.getValidator().validate(bad)).hasSizeGreaterThanOrEqualTo(8);
        }
    }
    @Test void disablingGuestPaymentHidesPayableQrAndBlocksInitiationAndImages() {
        seedCart();var online=new GuestCheckoutRequest(checkout.delivery(),checkout.idempotencyKey(),"BAKONG",checkout.expectedTotal());
        var confirmation=orders.create(session,online);long id=(Long)confirmation.order().get("id");
        when(paymentClient.guestQr(id)).thenAnswer(i->Map.of("status","PENDING","qr","server-generated-qr"));
        when(access.current()).thenReturn(new StorefrontAccess(true,false,true,true,false,true,false,true));
        var status=(Map<?,?>)orders.payment(session,id,confirmation.orderAccessToken(),false);
        assertThat(status.get("paymentAllowed")).isEqualTo(false);assertThat(status.containsKey("qr")).isFalse();
        assertThatThrownBy(()->orders.payment(session,id,confirmation.orderAccessToken(),true)).hasMessageContaining("403");
        assertThatThrownBy(()->orders.paymentImage(session,id,confirmation.orderAccessToken())).hasMessageContaining("403");
        verify(paymentClient,never()).guestImage(anyLong());
    }

}
