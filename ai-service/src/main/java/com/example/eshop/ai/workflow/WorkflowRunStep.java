package com.example.eshop.ai.workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="ai_workflow_run_steps",uniqueConstraints=@UniqueConstraint(columnNames={"run_id","position"})) @Getter @Setter
public class WorkflowRunStep {
 @Id private UUID id;
 @Column(nullable=false) private UUID runId;
 @Column(nullable=false) private int position;
 @Column(nullable=false,length=120) private String stepKey;
 @Column(nullable=false,length=120) private String name;
 @Column(nullable=false,length=80) private String tool;
 @Column(nullable=false,length=24) private String status;
 private Instant startedAt;
 private Instant finishedAt;
 @Column(length=80) private String errorCode;
 // Only response evidence is persisted; customer/provider payloads are never stored here.
 @Column(length=64) private String outputSha256;
 private Integer outputBytes;
}
