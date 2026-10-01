package com.example.eshop.order.model;

import com.example.eshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_items")
public class CartItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Was `ProductSku productSku` (@ManyToOne) — catalog-service owns ProductSku. Keep only the id.
    // TODO: cross-service call via Feign to catalog-service to resolve SKU name/image/price.
    @Column(name = "product_sku_id", nullable = false)
    private Long productSkuId;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;
}
