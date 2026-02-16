package com.example.learning_spring_security.Model;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PreRemove
    protected void onRemove() {
        deletedAt = LocalDateTime.now();
    }
    @OneToOne(mappedBy = "payment")
    private OrderDetail orderDetail;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CREDIT_CARD, PAYPAL, COD, etc.

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "payment_provider")
    private String paymentProvider; // STRIPE, PAYPAL, etc.

    @Column(name = "payment_provider_response", length = 2000)
    private String paymentProviderResponse;
}