package com.example.eshop.catalog.model;

import com.example.eshop.catalog.dto.request.ProductAttributeValueRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "attributes", indexes = @Index(name = "idx_attribute_sku_id", columnList = "product_sku_id,id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;
    private Long productSkuId;

}

