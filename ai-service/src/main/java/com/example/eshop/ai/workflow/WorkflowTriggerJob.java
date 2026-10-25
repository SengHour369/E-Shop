package com.example.eshop.ai.workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="ai_workflow_trigger_jobs",indexes=@Index(name="workflow_job_queue",columnList="status,createdAt")) @Getter @Setter
public class WorkflowTriggerJob {
 @Id private UUID id;@Version private Long revision;
 @Column(nullable=false) private UUID triggerId;
 @Column(nullable=false) private UUID versionId;
 @Column(nullable=false) private long ownerId;
 @Column(nullable=false,length=24) private String status;
 @Column(nullable=false,length=40) private String source;
 @Column(nullable=false) private UUID eventKey;
 @Column(nullable=false) private Instant createdAt;
 private Instant startedAt;private Instant finishedAt;private UUID executionId;
 @Column(length=80) private String errorCode;
 @Column(length=128) private String requestId;
}
