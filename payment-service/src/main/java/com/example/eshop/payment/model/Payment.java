package com.example.eshop.payment.model;

import com.example.eshop.common.entity.BaseEntity;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-service reference: order used to be a @OneToOne JPA relation to OrderDetail
    // (order-service). Flattened to a plain id column.
    // TODO: cross-service call via Feign to order-service to fetch order details (order number, etc.)
    @Column(name = "order_id")
    private Long orderId;

    // Cross-service reference: previously reached via orderDetail.getUser().getId() (auth-service).
    // TODO: cross-service call via Feign to auth-service if user details are needed beyond the id.
    @Column(name = "user_id")
    private Long userId;

    private String code;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CREDIT_CARD, PAYPAL, COD, etc.

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    private String codeOrder;

    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "payment_provider")
    private String paymentProvider; // STRIPE, PAYPAL, etc.

    @Column(name = "payment_provider_response", length = 2000)
    private String paymentProviderResponse;

    @Column(name = "qr_code", length = 2000)
    private String qrCode;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    private Boolean deleted = false;
}
