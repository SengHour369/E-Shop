package com.example.learning_spring_security.Model;

import com.example.learning_spring_security.Model.BaseEtity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;

@Getter
@Setter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "tbl_user")
public class User extends BaseEntity implements Serializable {

    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "full_name")
    private String fullName;
    private int attempt;
    private String status;
   // @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
   // private List<Address> addresses;
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


}