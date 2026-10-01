package com.example.eshop.catalog.model;

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
@Table(name = "product_skus")
public class ProductSku extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column( nullable = false)
    private String sku;

    @Column( nullable = false,columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "is_default")
    private Boolean isDefault = false;
    @Column(name = "operator_product_attribute")
    private Boolean OperatorProductAttribute = false;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Image image;

    // TODO: cross-service call via Feign to order-service — CartItem and OrderItem now live in
    // order-service's own schema and reference this SKU only by productSkuId (Long), not via a
    // JPA relation. If order-service needs SKU details it should call catalog-service instead.
}