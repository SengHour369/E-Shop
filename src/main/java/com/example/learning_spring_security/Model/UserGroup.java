package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_groups")
public class UserGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_code", length = 10, unique = true)
    private String groupCode;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "display")
    private String display;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_delete")
    private Boolean isDelete = false;
}