package com.example.eshop.order.model;

import com.example.eshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_details", uniqueConstraints = @UniqueConstraint(name = "uk_order_checkout", columnNames = {"user_id", "checkout_key"}))
public class OrderDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 100) private String checkoutKey;
    @Column(nullable = false) private boolean catalogCompleted;
    private LocalDateTime checkoutRetryAt;

    // Was `User user` (@ManyToOne) — auth-service owns User. Keep only the id.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Was `Payment payment` (@OneToOne) — payment-service owns Payment.
    // TODO: cross-service call via Feign to payment-service for payment details/status.
    @Column(name = "payment_id")
    private Long paymentId;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // Was `Address shippingAddress` (@ManyToOne) — auth-service owns Address.
    // TODO: cross-service call via Feign to auth-service to resolve address details.
    @Column(name = "shipping_address_id")
    private Long shippingAddressId;

    private Boolean deleted = false;
}
