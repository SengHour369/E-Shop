package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    private Boolean isActive = true;
    private Boolean isDelete = false;
}