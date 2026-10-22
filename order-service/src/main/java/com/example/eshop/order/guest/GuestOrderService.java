package com.example.eshop.order.guest;

import com.example.eshop.order.model.*;
import com.example.eshop.order.repository.*;
import com.example.eshop.order.mapper.OrderItemMapper;
import com.example.eshop.order.service.impl.*;
import com.example.eshop.common.storefront.*;
import com.example.eshop.common.exception.*;
import com.example.eshop.common.dto.CheckoutRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import com.example.eshop.order.automation.PaymentAutomationClient;
import com.example.eshop.common.automation.AutomationEvents;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class GuestOrderService {
    private final com.example.eshop.order.util.OrderCodeGenerator orderCodes;
    private final com.example.eshop.common.audit.AuditLogService audit;
    private final OrderRepository orders;
    private final CartRepository carts;
    private final GuestOrderAccessRepository accessTokens;
    private final GuestSessionCookieService cookies;
    private final StorefrontAccessClient access;
    private final CatalogAccess catalog;
    private final CheckoutCoordinator checkout;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final ObjectProvider<AutomationEvents> events;
    private final ObjectProvider<PaymentAutomationClient> payments;
    private StorefrontAccess checkoutAccess(GuestSession session,GuestCheckoutRequest request) {
        var settings=access.current(); access.require(settings.guestCheckout()&&settings.guestCart());
        if(!"CASH_ON_DELIVERY".equals(request.paymentMethod())) {
            access.require(settings.guestPayment());
            if(events.getIfAvailable()==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Payment automation unavailable");
        }
        if(settings.requireGuestContactVerification() && (session.getContactVerifiedAt()==null
                || request.delivery().email()==null || !request.delivery().email().equalsIgnoreCase(session.getVerifiedEmail())))
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Verify your contact first");
        return settings;
    }
    public Object preview(GuestSession session,GuestCheckoutRequest request) {
        checkoutAccess(session,request);
        return tx.execute(status -> {
            var cart=carts.findByGuestSessionId(session.getId()).orElseThrow(()->new BusinessLogicException("Cart is empty"));
            if(cart.getCheckoutOrderId()!=null) throw new BusinessLogicException("Checkout is already processing");
            return catalog.guestQuote(session.getId(),cart.getCartItems().stream().map(i->new CheckoutRequest.Line(i.getProductSkuId(),i.getQuantity())).toList());
        });
    }
    public record Confirmation(Map<String,Object> order,String orderAccessToken,Instant accessExpiresAt) { }
    public Confirmation create(GuestSession session,GuestCheckoutRequest request) {
        Long id=tx.execute(status -> {
            var cart=carts.findByGuestSessionId(session.getId()).orElseThrow(()->new BusinessLogicException("Cart is empty"));
            String snapshot;
            try { snapshot=json.writeValueAsString(request.delivery()); }
            catch(Exception e) { throw new IllegalStateException(e); }
            var prior=orders.findByGuestSessionIdAndCheckoutKey(session.getId(),request.idempotencyKey());
            if(prior.isPresent()) {
                var order=prior.get();
                if(!Objects.equals(order.getPaymentMethod(),request.paymentMethod())
                        || !Objects.equals(order.getShippingAddressSnapshot(),snapshot)
                        || order.getExpectedTotal().compareTo(request.expectedTotal())!=0)
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key already used for another checkout");
                return order.getId();
            }
            checkoutAccess(session,request);
            if(cart.getCheckoutOrderId()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Checkout is already processing");
            if(cart.getCartItems().isEmpty()) throw new BusinessLogicException("Cart is empty");
            var order=OrderDetail.builder().guestSessionId(session.getId()).checkoutKey(request.idempotencyKey())
                    .shippingAddressSnapshot(snapshot)
                    .verifiedContactEmail(session.getContactVerifiedAt()!=null && request.delivery().email()!=null && request.delivery().email().equalsIgnoreCase(session.getVerifiedEmail()) ? session.getVerifiedEmail() : null)
                    .paymentMethod(request.paymentMethod()).expectedTotal(request.expectedTotal())
                    .orderNumber(orderCodes.generateOrderCode()).orderDate(LocalDateTime.now()).status("CHECKOUT_PENDING")
                    .totalAmount(BigDecimal.ZERO).catalogCompleted(false).build();
            order.setOrderItems(cart.getCartItems().stream().map(i->OrderItemMapper.toEntity(order,i.getProductSkuId(),i.getQuantity(),BigDecimal.ZERO)).toList());
            orders.saveAndFlush(order);cart.setCheckoutOrderId(order.getId());
            audit.record(new com.example.eshop.common.audit.AuditEvent(com.example.eshop.common.audit.AuditAction.CREATE,"GUEST_ORDER",order.getId().toString(),null,
                    Map.of("customerType","GUEST","status","CHECKOUT_PENDING","paymentMethod",request.paymentMethod()),com.example.eshop.common.audit.AuditResult.SUCCESS,null));
            return order.getId();
        });
        // The durable row remains recoverable after a network failure; never create another order on retry.
        try { checkout.process(id); } catch(RuntimeException ignored) { }
        return issue(session,id);
    }
    public Confirmation issue(GuestSession session,long id) {
        return tx.execute(status -> {
            var order=owned(session,id);
            requireVisible(order);
            String token=cookies.randomToken();
            var credential=accessTokens.findByOrderIdAndGuestSessionId(id,session.getId()).orElseGet(GuestOrderAccess::new);
            credential.setOrderId(id);credential.setGuestSessionId(session.getId());
            credential.setTokenHash(GuestSessionCookieService.hash(token));
            credential.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            accessTokens.saveAndFlush(credential);
            return new Confirmation(view(order),token,credential.getExpiresAt());
        });
    }
    public Map<String,Object> detail(GuestSession session,long id,String token) {
        return tx.execute(status -> { requireToken(session,id,token);var order=owned(session,id);requireVisible(order);return view(order); });
    }
    public List<Map<String,Object>> list(GuestSession session) {
        access.require(access.current().guestOrderTracking());
        return tx.execute(status -> orders.findTop50ByGuestSessionIdOrderByIdDesc(session.getId()).stream().map(this::view).toList());
    }
    public Object payment(GuestSession session,long id,String token,boolean initiate) {
        return tx.execute(status -> {
            requireToken(session,id,token); var order=owned(session,id);
            var settings=access.current();boolean allowed=settings.guestCheckout()&&settings.guestPayment();
            if(initiate) access.require(allowed);
            if("CASH_ON_DELIVERY".equals(order.getPaymentMethod())) return Map.of("status","CASH_ON_DELIVERY","paymentVerified",false);
            var client=payments.getIfAvailable();
            if(client==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            // Creation already emits the payment event through the existing outbox. This only reads authoritative status.
            var result=new LinkedHashMap<String,Object>();client.guestQr(id).forEach((key,value)->result.put(key.toString(),value));
            result.put("paymentAllowed",allowed);if(!allowed) result.remove("qr");return result;
        });
    }
    public byte[] paymentImage(GuestSession session,long id,String token) {
        return tx.execute(status -> {
            requireToken(session,id,token); owned(session,id);
            var settings=access.current();access.require(settings.guestCheckout()&&settings.guestPayment());
            var client=payments.getIfAvailable(); if(client==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            return client.guestImage(id);
        });
    }
    private void requireVisible(OrderDetail order) {
        boolean confirmation=order.getOrderDate().isAfter(LocalDateTime.now().minusMinutes(15));
        access.require(confirmation || access.current().guestOrderTracking());
    }
    private OrderDetail owned(GuestSession session,long id) {
        var order=orders.findByIdWithItems(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(order.getUserId()!=null||!session.getId().equals(order.getGuestSessionId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return order;
    }
    private void requireToken(GuestSession session,long id,String token) {
        var credential=accessTokens.findByOrderIdAndGuestSessionId(id,session.getId())
                .filter(c->c.getExpiresAt().isAfter(Instant.now())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}")||!MessageDigest.isEqual(credential.getTokenHash().getBytes(StandardCharsets.US_ASCII),
                GuestSessionCookieService.hash(token).getBytes(StandardCharsets.US_ASCII))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private Map<String,Object> view(OrderDetail order) {
        var result=new LinkedHashMap<String,Object>();
        result.put("id",order.getId());result.put("orderNumber",order.getOrderNumber());result.put("status",order.getStatus());
        result.put("totalAmount",order.getTotalAmount());result.put("currency","USD");result.put("paymentMethod",order.getPaymentMethod());
        result.put("createdAt",order.getOrderDate());result.put("customerType","GUEST");
        result.put("items",order.getOrderItems().stream().map(i->Map.of("skuId",i.getProductSkuId(),"quantity",i.getQuantity(),"total",i.getTotalPrice())).toList());
        // Contact and address never appear on tracking responses.
        return result;
    }
}
