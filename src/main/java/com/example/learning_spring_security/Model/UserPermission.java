package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_permission_id")
    private Long userPermissionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "func_id", nullable = false)
    private Long funcId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;
}