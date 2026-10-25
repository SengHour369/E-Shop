package com.example.eshop.ai.workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="ai_workflow_triggers",uniqueConstraints=@UniqueConstraint(columnNames="workflow_id")) @Getter @Setter
public class WorkflowTrigger {
 @Id private UUID id;@Version private Long revision;
 @Column(nullable=false) private UUID workflowId;
 @Column(nullable=false) private UUID versionId;
 @Column(nullable=false) private long ownerId;
 @Column(nullable=false,length=16) private String triggerType;
 @Column(nullable=false,length=32) private String state;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false) private UUID grantId;
 @Column(nullable=false) private Instant authorizationExpiresAt;
 @Column(nullable=false) private Instant activatedAt;
 private Instant nextRunAt;private Integer intervalMinutes;
 @Column(length=40) private String eventType;@Column(length=120) private String eventName;private Long orderId;
 private Instant lastRunAt;@Column(length=32) private String lastStatus;
}
