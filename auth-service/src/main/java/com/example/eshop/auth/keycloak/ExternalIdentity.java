package com.example.eshop.auth.keycloak;

import com.example.eshop.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Provision explicitly; never infer identity ownership from email or realm roles. */
@Entity
@Table(
        name = "external_identity",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"issuer", "subject"}
        )
)
@Getter
@NoArgsConstructor
public class ExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String subject;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}