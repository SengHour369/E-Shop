package com.example.eshop.notification.service;
import com.example.eshop.notification.client.AuthMailClient;
import com.example.eshop.notification.model.*;
import com.example.eshop.notification.repository.*;
import com.example.eshop.common.notification.NotificationEvent;
import com.example.eshop.common.jwt.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor
public class EmailNotificationService {
    private final NotificationRepository repository;
    private final NotificationPreferenceRepository preferences;
    private final AuthMailClient mail;
    private final JwtProperties jwt;
    @Scheduled(fixedDelayString="${notifications.email-delay-ms:5000}") @Transactional
    public void deliver() {
        for(var n:repository.emailDue(Instant.now(),PageRequest.of(0,1))) {
            if(!preferences.findById(n.getUserId()).map(NotificationPreference::isAiEmail).orElse(false)) { n.setEmailStatus(Notification.EmailStatus.DISABLED); continue; }
            var previous=MDC.getCopyOfContextMap();
            try {
                MDC.clear(); MDC.put("requestId",n.getRequestId());
                if(n.getTraceId()!=null) MDC.put("traceId",n.getTraceId());
                String token="Bearer "+Jwts.builder().setSubject("notification-service")
                    .claim("authorities",List.of("SERVICE_NOTIFICATION")).claim("isEnable",true).claim("type","access")
                    .setIssuedAt(new Date()).setExpiration(Date.from(Instant.now().plusSeconds(60)))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();
                mail.send(token,new NotificationEvent(1,n.getEventId(),n.getRequestId(),n.getTraceId(),null,
                    n.getAiExecutionId(),n.getUserId(),n.getType(),n.getResourceType(),n.getResourceId(),n.getCreatedAt()));
                n.setEmailStatus(Notification.EmailStatus.SENT); n.setEmailedAt(Instant.now());
            } catch(Exception ex) {
                n.setEmailAttempts(n.getEmailAttempts()+1);
                if(n.getEmailAttempts()>=5) n.setEmailStatus(Notification.EmailStatus.FAILED);
                else n.setNextEmailAttemptAt(Instant.now().plusSeconds(60L*n.getEmailAttempts()));
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Email delivery deferred eventId={}",n.getEventId());
            } finally { MDC.clear(); if(previous!=null) MDC.setContextMap(previous); }
        }
    }
}
