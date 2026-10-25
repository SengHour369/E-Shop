package com.example.eshop.ai.workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="ai_workflow_received_events") @Getter @Setter
public class WorkflowReceivedEvent {
 @Id private UUID id;
 @Column(nullable=false) private Instant receivedAt;
 @Column(nullable=false,length=40) private String eventType;
}
