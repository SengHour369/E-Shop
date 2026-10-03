package com.example.eshop.auth.service;
import com.example.eshop.auth.config.InternalNotificationSecurityConfig;
import com.example.eshop.auth.controller.InternalNotificationController;
import com.example.eshop.auth.service.impl.InternalNotificationMailService;
import com.example.eshop.auth.repository.NotificationMailDeliveryRepository;
import com.example.eshop.common.jwt.*;
import com.example.eshop.common.audit.AuditSecurityHandlers;
import com.example.eshop.common.notification.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

import java.io.IOException;
import java.util.*;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=InternalNotificationSecurityTest.Config.class,properties={
 "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576",
 "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
 "eureka.client.enabled=false","spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class InternalNotificationSecurityTest {
    @SpringBootConfiguration @EnableAutoConfiguration @EnableMethodSecurity
    @Import({InternalNotificationSecurityConfig.class,InternalNotificationController.class,JwtProperties.class,JwtTokenValidator.class})
    static class Config {}
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean InternalNotificationMailService service;
    @MockitoBean NotificationMailDeliveryRepository deliveries;
    @MockitoBean AuditSecurityHandlers handlers;
    @BeforeEach void setup() throws IOException {
        doAnswer(c->{((jakarta.servlet.http.HttpServletResponse)c.getArgument(1)).setStatus(c.getArgument(2)); return null;})
            .when(handlers).reject(any(),any(),anyInt());
    }
    String token(String subject,String role,String type) {
        return "Bearer "+Jwts.builder().setSubject(subject).claim("authorities",List.of(role))
            .claim("isEnable",true).claim("type",type).setExpiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"))).compact();
    }
    String event() throws Exception { return mapper.writeValueAsString(new NotificationEvent(1,UUID.randomUUID(),"req_internal",null,null,UUID.randomUUID(),7,NotificationEvent.Type.AI_ACTION_COMPLETED,"PROMOTION","1",Instant.now())); }
    @Test void signedServiceTokenDoesNotNeedHumanAccount() throws Exception {
        mvc.perform(post("/internal/notifications/email").header("Authorization",token("notification-service","SERVICE_NOTIFICATION","access"))
            .contentType(MediaType.APPLICATION_JSON).content(event())).andExpect(status().isOk());
        verify(service).send(any());
    }
    @Test void humanAdminAndRefreshTokensCannotSendMail() throws Exception {
        mvc.perform(post("/internal/notifications/email").header("Authorization",token("admin","ADMIN","access"))
            .contentType(MediaType.APPLICATION_JSON).content(event())).andExpect(status().isForbidden());
        mvc.perform(post("/internal/notifications/email").header("Authorization",token("notification-service","SERVICE_NOTIFICATION","refresh"))
            .contentType(MediaType.APPLICATION_JSON).content(event())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
