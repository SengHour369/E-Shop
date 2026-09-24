package com.example.learning_spring_security.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "attribute_values",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attribute_id", "value"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String value;
    private Long attributeId;

}

