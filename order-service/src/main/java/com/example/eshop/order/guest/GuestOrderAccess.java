package com.example.eshop.order.guest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="guest_order_access",uniqueConstraints=@UniqueConstraint(columnNames={"orderId","guestSessionId"}))
@Getter @Setter
public class GuestOrderAccess {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false) private Long orderId;
    @Column(nullable=false) private UUID guestSessionId;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
}
