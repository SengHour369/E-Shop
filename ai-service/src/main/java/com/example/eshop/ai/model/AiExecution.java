package com.example.eshop.ai.model;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_executions", indexes = {
        @Index(name = "ai_execution_owner_time", columnList = "userId,startedAt"),
        @Index(name = "ai_execution_request", columnList = "requestId")
})
@Getter
@Setter
public class AiExecution {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private long userId;

    @Column(length = 128, nullable = false)
    private String requestId;

    @Column(length = 32)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AiIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private AiExecutionStatus status;

    @Column(length = 80)
    private String errorCode;

    @Column(length = 100)
    private String resourceId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;
}
