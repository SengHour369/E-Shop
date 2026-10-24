package com.example.eshop.ai.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ai_workflow_published_versions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"workflow_id", "draft_revision"}
        )
)
@Getter
@Setter
public class WorkflowPublishedVersion {

 @Id
 private UUID id;

 @Column(nullable = false, updatable = false)
 private UUID workflowId;

 @Column(nullable = false, updatable = false)
 private long draftRevision;

 @Column(nullable = false, updatable = false)
 private long ownerId;

 @Column(nullable = false, updatable = false, length = 120)
 private String name;

 @Column(nullable = false, updatable = false, columnDefinition = "text")
 private String definition;

 @Column(nullable = false, updatable = false)
 private Instant publishedAt;
}