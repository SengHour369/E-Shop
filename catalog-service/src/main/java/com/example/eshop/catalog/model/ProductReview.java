package com.example.eshop.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "product_reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"productId", "guestSessionId"}),
                @UniqueConstraint(columnNames = {"productId", "userId"})
        }
)
@Getter
@Setter
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    private UUID guestSessionId;

    private Long userId;

    @Column(nullable = false, length = 80)
    private String author;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private String moderationStatus = "PENDING";

    @Column(nullable = false)
    private boolean verifiedPurchase;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Long moderatedBy;

    private Instant moderatedAt;

    @Version
    private long version;
}