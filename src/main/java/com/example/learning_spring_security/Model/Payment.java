package com.example.learning_spring_security.Model;

import com.example.learning_spring_security.Model.BaseEntity.BaseEntity;
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
public class Payment extends BaseEntity {

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