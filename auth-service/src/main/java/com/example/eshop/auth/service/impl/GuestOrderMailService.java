package com.example.eshop.auth.service.impl;
import com.example.eshop.auth.repository.GuestMailDeliveryRepository;
import com.example.eshop.auth.model.GuestMailDelivery;
import com.example.eshop.common.notification.*;
import com.example.eshop.common.automation.ServiceIdentity;
import com.example.eshop.common.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class GuestOrderMailService {
    private final GuestMailDeliveryRepository deliveries;
    private final EmailNotificationService email;
    private final JwtProperties jwt;
    @Value("${automation.order-url:http://order-service:8083}") private String orderUrl;
    @Transactional
    public void send(GuestNotificationEvent event) {
        event.validate();if(deliveries.existsById(event.eventId())) return;
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(5000);
        var notice=RestClient.builder().baseUrl(orderUrl).requestFactory(factory).build().get()
                .uri("/internal/commerce/guest-orders/{id}/notice",event.orderId())
                .header("Authorization",ServiceIdentity.bearer(jwt,"auth-service","SERVICE_NOTIFICATION"))
                .retrieve().body(GuestOrderNotice.class);
        // No email is sent unless the order captured this exact verified recipient at checkout.
        if(notice==null||notice.verifiedEmail()==null||notice.verifiedEmail().isBlank()) return;
        if(event.type()==NotificationEvent.Type.PAYMENT_SUCCESS&&!notice.paymentVerified())
            throw new IllegalStateException("Payment verification unavailable");
        var delivery=new GuestMailDelivery();delivery.setEventId(event.eventId());delivery.setOrderId(event.orderId());delivery.setSentAt(Instant.now());deliveries.saveAndFlush(delivery);
        email.sendSimpleEmail(notice.verifiedEmail(),"E-Shop guest order update",
                "Order: "+notice.orderNumber()+"\nCurrent status: "+notice.status()+"\nTotal: "+notice.amount()+" "+notice.currency()
                +(notice.paymentVerified()?"\nPayment verified by the provider.":"\nPayment has not been confirmed by this notification.")
                +"\nOpen the store in the browser used for checkout to view your order.");
    }
}
