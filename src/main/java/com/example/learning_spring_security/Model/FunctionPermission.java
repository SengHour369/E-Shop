package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "function_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionPermission extends BaseEntity {

    @Id
    @Column(name = "func_id")
    private Long funcId;

    @Column(name = "func_code", unique = true, nullable = false, length = 50)
    private String funcCode;

    @Column(name = "func_name", nullable = false, length = 100)
    private String funcName;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String module;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;
}