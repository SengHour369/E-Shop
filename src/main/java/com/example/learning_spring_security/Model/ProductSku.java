package com.example.learning_spring_security.Model;


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

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "is_default")
    private Boolean isDefault = false;
    @Column(name = "operator_product_attribute")
    private Boolean OperatorProductAttribute = false;

    @OneToMany(mappedBy = "productSku", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();
    @OneToMany(mappedBy = "productSku", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();
}