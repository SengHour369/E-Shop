package com.example.eshop.auth.service;

import com.example.eshop.auth.service.impl.*;
import com.example.eshop.auth.repository.GuestMailDeliveryRepository;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.notification.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class GuestOrderMailPrivacyTest {
    HttpServer server;
    GuestMailDeliveryRepository deliveries;
    EmailNotificationService email;
    GuestOrderMailService service;
    GuestOrderNotice notice;
    int requests;
    @BeforeEach void setup() throws Exception {
        deliveries=mock(GuestMailDeliveryRepository.class);email=mock(EmailNotificationService.class);
        var jwt=new JwtProperties();ReflectionTestUtils.setField(jwt,"secret","66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576");
        service=new GuestOrderMailService(deliveries,email,jwt);
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/commerce/guest-orders/1/notice",exchange->{
            requests++;assertThat(exchange.getRequestHeaders().getFirst("Authorization")).startsWith("Bearer ");
            byte[] body=new ObjectMapper().writeValueAsBytes(notice);exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();ReflectionTestUtils.setField(service,"orderUrl","http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void close() {server.stop(0);}
    GuestNotificationEvent event(NotificationEvent.Type type) {return new GuestNotificationEvent(UUID.randomUUID(),1,type,"guest-test-request",null,Instant.now());}
    @Test void unverifiedContactReceivesNoOrderInformation() {
        notice=new GuestOrderNotice(null,"ORD-1","PENDING",BigDecimal.TEN,"USD",false);
        service.send(event(NotificationEvent.Type.ORDER_CREATED));verifyNoInteractions(email);verify(deliveries,never()).saveAndFlush(any());
    }
    @Test void recipientIsResolvedFromAuthoritativeOrderAndEventContainsNoEmail() throws Exception {
        notice=new GuestOrderNotice("verified@example.test","ORD-1","PROCESSING",BigDecimal.TEN,"USD",true);
        var event=event(NotificationEvent.Type.PAYMENT_SUCCESS);service.send(event);
        verify(email).sendSimpleEmail(eq("verified@example.test"),anyString(),contains("Payment verified by the provider"));
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(event)).doesNotContain("@","guestSession","token");
        verify(deliveries).saveAndFlush(any());
    }
    @Test void paymentNotificationCannotAssertUnverifiedSuccess() {
        notice=new GuestOrderNotice("verified@example.test","ORD-1","PENDING",BigDecimal.TEN,"USD",false);
        assertThatThrownBy(()->service.send(event(NotificationEvent.Type.PAYMENT_SUCCESS))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(email);
    }
    @Test void duplicateEventDoesNotSendAnotherMessage() {
        var event=event(NotificationEvent.Type.ORDER_CREATED);when(deliveries.existsById(event.eventId())).thenReturn(true);
        service.send(event);verifyNoInteractions(email);assertThat(requests).isZero();
    }
}
