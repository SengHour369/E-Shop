package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "quantity_change", nullable = false)
    private Long quantityChange; // positive = increase, negative = decrease

    @Column(name = "previous_quantity")
    private Long previousQuantity;

    @Column(name = "new_quantity")
    private Long newQuantity;

    @Column(name = "movement_type", length = 50)
    private String movementType; // RESTOCK, ADJUSTMENT, ORDER_PLACED, ORDER_CANCELLED, RETURN, INITIAL

    private String remark;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "warehouse_location")
    private String warehouseLocation;
}