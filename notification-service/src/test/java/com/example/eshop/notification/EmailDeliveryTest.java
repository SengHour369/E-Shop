package com.example.eshop.notification;
import com.example.eshop.notification.client.AuthMailClient;
import com.example.eshop.notification.model.*;
import com.example.eshop.notification.repository.*;
import com.example.eshop.notification.service.EmailNotificationService;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.notification.NotificationEvent;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import java.util.*;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class EmailDeliveryTest {
    NotificationRepository repository=mock(NotificationRepository.class);
    NotificationPreferenceRepository preferences=mock(NotificationPreferenceRepository.class);
    AuthMailClient mail=mock(AuthMailClient.class);
    JwtProperties jwt=mock(JwtProperties.class);
    EmailNotificationService service=new EmailNotificationService(repository,preferences,mail,jwt);
    Notification n;
    @BeforeEach void setup() {
        when(jwt.getSecret()).thenReturn("66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576");
        n=new Notification(); n.setEventId(UUID.randomUUID()); n.setAiExecutionId(UUID.randomUUID()); n.setUserId(7);
        n.setRequestId("req_mail"); n.setType(NotificationEvent.Type.AI_ACTION_COMPLETED); n.setResourceType("PROMOTION"); n.setCreatedAt(Instant.now());
        n.setEmailStatus(Notification.EmailStatus.PENDING);
        when(repository.emailDue(any(),any())).thenReturn(List.of(n));
        var p=new NotificationPreference(); p.setUserId(7L); p.setAiEmail(true); when(preferences.findById(7L)).thenReturn(Optional.of(p));
    }
    @AfterEach void clear() { MDC.clear(); }
    @Test void successTracksDeliveryAndRestoresRequestContext() {
        MDC.put("requestId","previous");
        service.deliver();
        assertThat(n.getEmailStatus()).isEqualTo(Notification.EmailStatus.SENT);
        assertThat(n.getEmailedAt()).isNotNull();
        verify(mail).send(startsWith("Bearer "),argThat(e->e.requestId().equals("req_mail")&&e.userId()==7));
        assertThat(MDC.get("requestId")).isEqualTo("previous");
    }
    @Test void failureBacksOffAndStopsAfterFiveAttempts() {
        doThrow(new RuntimeException("smtp failure")).when(mail).send(anyString(),any());
        service.deliver();
        assertThat(n.getEmailAttempts()).isEqualTo(1); assertThat(n.getNextEmailAttemptAt()).isAfter(Instant.now());
        n.setEmailAttempts(4); service.deliver();
        assertThat(n.getEmailStatus()).isEqualTo(Notification.EmailStatus.FAILED);
    }
    @Test void optOutBeforeDispatchSuppressesMail() {
        when(preferences.findById(7L)).thenReturn(Optional.empty()); service.deliver();
        assertThat(n.getEmailStatus()).isEqualTo(Notification.EmailStatus.DISABLED);
        verifyNoInteractions(mail);
    }
}
