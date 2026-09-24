package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"method", "api"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 255)
    private String api;

    @Column(name = "func_id", nullable = false)
    private Long funcId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;
}