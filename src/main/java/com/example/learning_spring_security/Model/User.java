package com.example.learning_spring_security.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;


import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "tbl_user")
public class User extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "Invalid email format")
    private String email;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "full_name")
    private String fullName;
    private int attempt;
    @Column(name = "status", nullable = false)
    private String status;
    @OneToOne(cascade = CascadeType.ALL)
    private Image  image;
    private String birthdate;
    @Column(name = "deleted", nullable = false)
    private Boolean deleted =  false;
    @Column(name = "phone",unique = true,nullable = false)
    private String phone;
    @Column(name = "verification_code")
    private String verificationCode;
    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orders;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cart cart;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    private List<Address> addresses;


}