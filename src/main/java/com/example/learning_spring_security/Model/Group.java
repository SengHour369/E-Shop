package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tt_group")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 10)
    private String groupCode;

    private String name;
    private String description;
    private String status;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_delete")
    private Boolean isDelete = false;
}