package com.example.eshop.ai.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notification_outbox", indexes=@Index(name="outbox_pending",columnList="publishedAt,nextAttemptAt"))
@Getter @Setter
public class NotificationOutbox {
    @Id private UUID id;
    @Column(nullable=false,columnDefinition="text") private String payload;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant nextAttemptAt;
    private Instant publishedAt;
    private int attempts;
}
