package com.example.eshop.auth.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="guest_mail_deliveries") @Getter @Setter
public class GuestMailDelivery {
    @Id private UUID eventId;
    @Column(nullable=false) private long orderId;
    @Column(nullable=false) private Instant sentAt;
    @Version private Long version;
}
