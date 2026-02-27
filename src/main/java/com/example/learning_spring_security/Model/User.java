package com.example.learning_spring_security.Model;


import jakarta.persistence.*;
import lombok.*;


import java.io.Serializable;
import java.sql.Time;
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
public class User  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @PreRemove
    protected void onRemove() {
        deletedAt = LocalDateTime.now();
    }
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "full_name")
    private String fullName;
    private int attempt;
    @Column(name = "status", nullable = false)
    private String status;
    private String  image;
    private String birthdate;
    @Column(name = "created", updatable = false)
    private LocalDateTime created;
    @Column(name = "updated", insertable = false)
    private LocalDateTime updated;

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
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    private List<Address> addresses;


}