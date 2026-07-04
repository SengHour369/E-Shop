package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_permissions")
public class GroupPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_permission_id")
    private Long groupPermissionId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "func_id", nullable = false)
    private Long funcId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}