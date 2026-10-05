package com.example.eshop.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "store_settings")
@Getter
@Setter
public class StoreSettings {
    @Id
    private Long id;
    @Column(nullable = false, length = 120)
    private String storeName;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(length = 40)
    private String phone;
    @Column(length = 500)
    private String address;
    @Column(nullable = false, length = 80)
    private String timezone;
}
