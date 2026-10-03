package com.example.eshop.notification;
import com.example.eshop.notification.service.*;
import com.example.eshop.notification.consumer.NotificationEventConsumer;
import com.example.eshop.notification.repository.*;
import com.example.eshop.notification.model.Notification;
import com.example.eshop.common.notification.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
 "eureka.client.enabled=false","spring.cloud.discovery.enabled=false",
 "spring.kafka.admin.auto-create=false","spring.kafka.listener.auto-startup=false",
 "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"})
class NotificationIntegrationTest {
    @Autowired NotificationService service;
    @Autowired NotificationEventConsumer consumer;
    @Autowired NotificationRepository repository;
    @Autowired NotificationPreferenceRepository preferences;
    @Autowired ObjectMapper mapper;
    @MockitoBean EmailNotificationService email;
    @BeforeEach void setup() { repository.deleteAll(); preferences.deleteAll(); actor(7); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); MDC.clear(); }
    void actor(long id) {
        var auth=new UsernamePasswordAuthenticationToken("user",null,List.of());
        auth.setDetails(Map.of("userId",id)); SecurityContextHolder.getContext().setAuthentication(auth);
    }
    NotificationEvent event(long owner) {
        return new NotificationEvent(1,UUID.randomUUID(),"req_notification",null,null,UUID.randomUUID(),owner,
            NotificationEvent.Type.AI_ACTION_COMPLETED,"PROMOTION","77",Instant.now());
    }
    @Test void redeliveryCreatesOneNotificationAndRestoresContext() throws Exception {
        var event=event(7); MDC.put("requestId","previous");
        consumer.consume(mapper.writeValueAsString(event)); consumer.consume(mapper.writeValueAsString(event));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(MDC.get("requestId")).isEqualTo("previous");
        assertThat(repository.findAll().get(0).getRequestId()).isEqualTo(event.requestId());
    }
    @Test void ownerIsolationCountReadAndMarkAll() {
        service.receive(event(7)); service.receive(event(7)); service.receive(event(8));
        assertThat(service.unread()).isEqualTo(2);
        long id=service.list(1,20,null,null,null,null,null).getContent().get(0).id();
        service.markRead(id); service.markRead(id); assertThat(service.unread()).isEqualTo(1);
        assertThat(service.markAll()).isEqualTo(1); assertThat(service.unread()).isZero();
        actor(8); assertThat(service.unread()).isEqualTo(1);
        assertThatThrownBy(()->service.get(id)).hasMessageContaining("404");
        assertThatThrownBy(()->service.markRead(id)).hasMessageContaining("404");
    }
    @Test void paginationAndFiltersAreAppliedInDatabase() {
        for(int i=0;i<5;i++) service.receive(event(7));
        var page=service.list(2,2,Notification.Status.SENT,NotificationEvent.Type.AI_ACTION_COMPLETED,false,Instant.now().minusSeconds(60),Instant.now().plusSeconds(60));
        assertThat(page.getTotalElements()).isEqualTo(5); assertThat(page.getContent()).hasSize(2);
        assertThatThrownBy(()->service.list(1,101,null,null,null,null,null)).hasMessageContaining("400");
    }
    @Test void emailIsOptInAndDurablyPending() {
        service.receive(event(7));
        assertThat(repository.findAll().get(0).getEmailStatus()).isEqualTo(Notification.EmailStatus.DISABLED);
        service.preference(true); service.receive(event(7));
        assertThat(repository.findAll()).anyMatch(n->n.getEmailStatus()==Notification.EmailStatus.PENDING);
    }
    @Test void malformedEventsAreRejectedWithoutSaving() {
        assertThatThrownBy(()->consumer.consume("{\"version\":99}")).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.count()).isZero();
    }
}
