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
@Table(name = "tbl_order_cancelation")
public class OrderCancelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cancelation_id", unique = true)
    private String cancelationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "cancel_reason", nullable = false)
    private String cancelReason;

    @Column(name = "cancel_status", nullable = false)
    private String cancelStatus;

    @Column(name = "cancel_source")
    private String cancelSource;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency;

    @Column(length = 2000)
    private String remark;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}