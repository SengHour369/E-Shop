package com.example.learning_spring_security.Model;

import com.example.learning_spring_security.Model.BaseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String image;

    @Column(name = "main_image")
    private String mainImage;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id") // ប្តូរពី subCategories_id
    private SubCategory subCategory; // ប្តូរពី subCategories

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductSku> productSkus = new ArrayList<>();
}