package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Table(name = "images")
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
    @OneToOne(cascade = CascadeType.ALL)
    private User user;

}