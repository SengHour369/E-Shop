package com.example.eshop.ai.workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="ai_workflow_runs",indexes=@Index(name="workflow_run_owner_time",columnList="ownerId,startedAt")) @Getter @Setter
public class WorkflowRun {
 @Id private UUID id;
 @Version private Long revision;
 @Column(nullable=false) private UUID publishedVersionId;
 @Column(nullable=false) private long ownerId;
 @Column(nullable=false,length=24) private String status;
 @Column(nullable=false,length=128) private String requestId;
 @Column(length=128) private String traceId;
 @Column(nullable=false) private Instant startedAt;
 private Instant finishedAt;
}
