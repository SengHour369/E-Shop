package com.example.eshop.notification.model;
import com.example.eshop.common.notification.NotificationEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notifications", uniqueConstraints=@UniqueConstraint(name="notification_event_unique",columnNames="eventId"),
    indexes={@Index(name="notification_owner_time",columnList="userId,createdAt"),@Index(name="notification_owner_read",columnList="userId,readAt"),@Index(name="notification_request",columnList="requestId"),@Index(name="notification_email_due",columnList="emailStatus,nextEmailAttemptAt")})
@Getter @Setter
public class Notification {
    public enum Status { SENT, READ }
    public enum EmailStatus { DISABLED, PENDING, SENT, FAILED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,updatable=false) private UUID eventId;
    @Column(nullable=false,updatable=false) private long userId;
    @Enumerated(EnumType.STRING) @Column(length=40,nullable=false) private NotificationEvent.Type type;
    @Column(length=120,nullable=false) private String title;
    @Column(length=500,nullable=false) private String message;
    @Column(length=40,nullable=false) private String resourceType;
    @Column(length=100) private String resourceId;
    @Column(length=128,nullable=false) private String requestId;
    @Column(length=32) private String traceId;
    private UUID aiExecutionId;
    @Enumerated(EnumType.STRING) @Column(length=16,nullable=false) private Status status;
    @Enumerated(EnumType.STRING) @Column(length=16,nullable=false) private EmailStatus emailStatus;
    private int emailAttempts;
    private Instant nextEmailAttemptAt;
    private Instant emailedAt;
    private Instant readAt;
    @Column(nullable=false) private Instant sentAt;
    @Column(nullable=false) private Instant createdAt;
}
