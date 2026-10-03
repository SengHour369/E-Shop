package com.example.eshop.notification.dto;
import com.example.eshop.notification.model.Notification;
import com.example.eshop.common.notification.NotificationEvent;
import java.time.Instant;
public record NotificationResponse(Long id, NotificationEvent.Type type, String title, String message,
    String resourceType, String resourceId, String channel, Notification.Status status, boolean read,
    Instant readAt, Instant sentAt, Instant createdAt, String requestId) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(),n.getType(),n.getTitle(),n.getMessage(),n.getResourceType(),n.getResourceId(),
            "IN_APP",n.getStatus(),n.getReadAt()!=null,n.getReadAt(),n.getSentAt(),n.getCreatedAt(),n.getRequestId());
    }
}
