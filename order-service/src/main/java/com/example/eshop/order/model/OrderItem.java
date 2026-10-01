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
@Table(name = "order_items")
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id", nullable = false)
    private OrderDetail orderDetail;

    // Was `ProductSku productSku` (@ManyToOne) — catalog-service owns ProductSku. Keep only the id.
    // TODO: cross-service call via Feign to catalog-service to resolve SKU name/image.
    @Column(name = "product_sku_id", nullable = false)
    private Long productSkuId;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
    @Column(precision = 19, scale = 2) private BigDecimal baseUnitPrice;
    @Column(precision = 19, scale = 2) private BigDecimal discountAmount;
    @Column(precision = 19, scale = 2) private BigDecimal finalUnitPrice;
    private Long promotionId;
    private String promotionName;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;
}
