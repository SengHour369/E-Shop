package com.example.learning_spring_security.Model;


import com.example.learning_spring_security.Enumeration.PaymentMethod;
import com.example.learning_spring_security.Enumeration.TransactionStatus;
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
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_no", nullable = false, unique = true, length = 50)
    private String transactionNo;   // PAY-8D7F9A1K

    @Column(name = "order_id", nullable = false)
    private Long order;

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
    private TransactionStatus status;   // SUCCESSFUL, PENDING, FAILED, REFUNDED, CANCELLED

    @Column(name = "remarks", length = 255)
    private String remarks;

}