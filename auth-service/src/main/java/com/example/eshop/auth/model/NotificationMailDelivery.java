package com.example.eshop.auth.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notification_mail_deliveries") @Getter @Setter
public class NotificationMailDelivery {
    @Id private UUID eventId;
    @Version private Long version;
    @Column(nullable=false) private long userId;
    @Column(nullable=false) private Instant sentAt;
}
