package com.example.eshop.catalog.model;

import com.example.eshop.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Table(name = "images", indexes = @Index(name = "idx_image_product_id", columnList = "product_id,id"))
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Image extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "url_image")
    private String url;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "SubCategory_id")
    private SubCategory  subCategory;
    // TODO: cross-service call via Feign to auth-service — User now lives in auth-service;
    // keep only the owning user's id here instead of a cross-service JPA relation.
    @Column(name = "user_id")
    private Long userId;

}