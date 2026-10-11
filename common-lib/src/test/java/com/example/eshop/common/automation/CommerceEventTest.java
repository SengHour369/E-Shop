package com.example.eshop.common.automation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class CommerceEventTest {
    @Test void paymentRequiresAnIdentifier() {
        var event=new CommerceEvent(1,UUID.randomUUID(),CommerceEvent.Type.PAYMENT_VERIFIED,1,2,BigDecimal.TEN,"USD","BAKONG",null,"request-1",null,Instant.now());
        assertThatThrownBy(event::validate).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsUnknownVersionAndInvalidTrace() {
        var event=new CommerceEvent(2,UUID.randomUUID(),CommerceEvent.Type.ORDER_CREATED,1,2,BigDecimal.TEN,"USD","BAKONG",null,"request-1","untrusted",Instant.now());
        assertThatThrownBy(event::validate).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void lifecycleNotificationDoesNotRequireAnAiExecution() {
        var event=new com.example.eshop.common.notification.NotificationEvent(1,UUID.randomUUID(),"request-1",null,null,null,1,
                com.example.eshop.common.notification.NotificationEvent.Type.ORDER_CREATED,"ORDER","1",Instant.now());
        assertThatCode(event::validate).doesNotThrowAnyException();
    }
}
