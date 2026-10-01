package com.example.eshop.payment.model;

import com.example.eshop.common.entity.BaseEntity;
import com.example.eshop.payment.enumeration.PaymentMethod;
import com.example.eshop.payment.enumeration.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_no", nullable = false, unique = true, length = 50)
    private String transactionNo;   // PAY-8D7F9A1K

    // TODO: cross-service call via Feign to order-service if order details are needed.
    @Column(name = "order_id", nullable = false)
    private Long order;

    // TODO: cross-service call via Feign to auth-service if customer details are needed.
    @Column(name = "customer_id", nullable = false)
    private Long customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "masked_account", length = 50)
    private String maskedAccount; // **** 4242, **** 5555

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;   // SUCCESS, PENDING, FAILED, REFUNDED, CANCELLED

    @Column(name = "remarks", length = 255)
    private String remarks;

}
