package com.example.eshop.common.notification;
import java.util.UUID;
import java.time.Instant;

/** Transport contains no address, email, guest token or arbitrary message content. */
public record GuestNotificationEvent(UUID eventId,long orderId,NotificationEvent.Type type,String requestId,String traceId,Instant occurredAt) {
    public static final String TOPIC="notification.guest.v1";
    public void validate() {
        if(eventId==null||orderId<1||type==null||!java.util.Set.of(NotificationEvent.Type.ORDER_CREATED,NotificationEvent.Type.ORDER_UPDATED,
                NotificationEvent.Type.ORDER_CANCELLED,NotificationEvent.Type.PAYMENT_SUCCESS,NotificationEvent.Type.PAYMENT_FAILED).contains(type)
                ||requestId==null||!requestId.matches("[A-Za-z0-9._:-]{1,128}")||occurredAt==null
                ||traceId!=null&&!traceId.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Invalid guest notice");
    }
}
