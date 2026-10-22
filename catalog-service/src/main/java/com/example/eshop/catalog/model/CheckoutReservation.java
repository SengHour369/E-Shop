package com.example.eshop.catalog.model;

import com.example.eshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checkout_reservations")
@Getter
@Setter
public class CheckoutReservation extends BaseEntity {

    @Id
    private Long orderId;

    private Long userId;
    private java.util.UUID guestSessionId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, columnDefinition = "text")
    private String snapshot;
}