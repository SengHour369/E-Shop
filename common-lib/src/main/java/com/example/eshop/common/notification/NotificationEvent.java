package com.example.eshop.common.notification;

import java.time.Instant;
import java.util.UUID;

/** No prompt, arbitrary message, email address, token, or model output crosses this contract. */
public record NotificationEvent(int version, UUID eventId, String requestId, String traceId,
        String spanId, UUID aiExecutionId, long userId, Type type, String resourceType,
        String resourceId, Instant occurredAt) {
    public enum Type { AI_ACTION_COMPLETED, AI_ACTION_FAILED }
    public static final String TOPIC = "notification.events.v1";
    public void validate() {
        if (version != 1 || eventId == null || aiExecutionId == null || userId <= 0 || type == null || occurredAt == null
                || requestId == null || !requestId.matches("[A-Za-z0-9._:-]{1,128}")
                || (traceId != null && !traceId.matches("[a-f0-9]{32}"))
                || (spanId != null && !spanId.matches("[a-f0-9]{16}"))
                || resourceType == null || !resourceType.matches("[A-Z_]{1,40}")
                || (resourceId != null && !resourceId.matches("[A-Za-z0-9_-]{1,100}")))
            throw new IllegalArgumentException("Invalid notification event");
    }
}
