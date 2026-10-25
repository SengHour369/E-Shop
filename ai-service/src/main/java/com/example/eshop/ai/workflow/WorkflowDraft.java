package com.example.eshop.ai.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_workflow_drafts")
@Getter
@Setter
public class WorkflowDraft {

    @Id
    private UUID id;

    @Version
    private Long revision;

    @Column(nullable = false)
    private long ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String definition;

    @Column(length = 120)
    private String moduleCode;

    @Column(length = 32)
    private String triggerType;

    @Column(nullable = false)
    private Instant updatedAt;
}