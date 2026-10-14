package com.example.eshop.catalog.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="catalog_branch_access",uniqueConstraints=@UniqueConstraint(columnNames={"branch_id","user_id"})) @Getter @Setter
public class BranchAccess {
 @Id private UUID id=UUID.randomUUID();
 @Column(name="branch_id",nullable=false) private Long branchId;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(nullable=false) private boolean active;
 @Column(nullable=false) private Long updatedBy;
 @Column(nullable=false) private Instant updatedAt=Instant.now();
}
