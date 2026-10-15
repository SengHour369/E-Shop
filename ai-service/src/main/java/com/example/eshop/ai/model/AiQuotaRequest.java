package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ai_quota_request")
@Getter
@Setter
public class AiQuotaRequest {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant acceptedAt;

    @Column(length = 128)
    private String requestId;

    @Column(length = 128)
    private String traceId;

    @Column(length = 4)
    private String feedback;

    private Integer httpStatus;

    @Column(columnDefinition = "text")
    private String responseJson;
}