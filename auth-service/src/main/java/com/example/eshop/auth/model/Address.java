package com.example.eshop.auth.model;

import com.example.eshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "addresses")
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label", length = 40)
    private String label;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "commune", length = 100)
    private String commune;

    @Column(name = "additional_information", length = 500)
    private String additionalInformation;



    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(nullable = false)
    private String city;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @ManyToMany(mappedBy = "addresses")
    private List<User> users;

    private Boolean deleted = false;
}
