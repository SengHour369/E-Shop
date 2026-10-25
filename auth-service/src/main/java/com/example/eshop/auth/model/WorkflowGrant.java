package com.example.eshop.auth.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="workflow_authorization_grants") @Getter @Setter
public class WorkflowGrant {
 @Id private UUID id;
 @Column(nullable=false) private long ownerId;
 @Column(nullable=false) private UUID versionId;
 @Column(nullable=false) private Instant expiresAt;
 @Column(nullable=false) private boolean revoked;
 @Column(nullable=false) private Instant createdAt;
}
