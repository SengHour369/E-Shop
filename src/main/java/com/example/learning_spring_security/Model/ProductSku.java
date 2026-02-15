package com.example.learning_spring_security.Model;

import com.example.learning_spring_security.Model.BaseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_skus")
public class ProductSku extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(unique = true, nullable = false)
    private String sku;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "compare_price")
    private BigDecimal comparePrice;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    private String color;
    private String size;
    private String material;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "productSku", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "productSku", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();
}