package com.example.eshop.order.guest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="guest_sessions", indexes=@Index(name="ix_guest_expiry",columnList="expiresAt"))
@Getter @Setter
public class GuestSession {
    @Version private long version;
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false,length=64) private String csrfHash;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private Instant rotatedAt = Instant.now();
    private Instant contactVerifiedAt;
    @Column(length=254) private String verifiedEmail;
}
