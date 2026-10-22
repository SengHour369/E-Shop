package com.example.eshop.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="storefront_access_settings") @Getter @Setter
public class StorefrontAccessSettings {
    @Id private Long id = 1L;
    @Version private long version;
    @Column(nullable=false) private boolean guestBrowsing = true;
    @Column(nullable=false) private boolean guestReviews;
    @Column(nullable=false) private boolean guestCart;
    @Column(nullable=false) private boolean guestCheckout;
    @Column(nullable=false) private boolean guestPayment;
    @Column(nullable=false) private boolean requireDeliveryAddress = true;
    @Column(nullable=false) private boolean requireGuestContactVerification;
    @Column(nullable=false) private boolean guestOrderTracking;
    @Column(nullable=false) private Instant updatedAt = Instant.now();
    private Long updatedBy;
}
