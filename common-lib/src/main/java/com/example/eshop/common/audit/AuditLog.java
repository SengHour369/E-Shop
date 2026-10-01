package com.example.eshop.common.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity @Immutable @Getter @NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_request", columnList = "request_id"),
    @Index(name = "idx_audit_trace", columnList = "trace_id"),
    @Index(name = "idx_audit_actor_time", columnList = "actor_id,occurred_at"),
    @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
    @Index(name = "idx_audit_time", columnList = "occurred_at,id")})
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String requestId;
    @Column(length = 128) private String traceId;
    @Column(length = 128) private String actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AuditActorType actorType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 64) private AuditAction action;
    @Column(nullable = false, length = 64) private String resourceType;
    @Column(length = 128) private String resourceId;
    @Column(length = 256) private String description;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String oldValue;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String newValue;
    @Column(length = 64) private String ipAddress;
    @Column(length = 512) private String userAgent;
    @Column(nullable = false, length = 128) private String serviceName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AuditResult result;
    @Column(length = 64) private String errorCode;
    @Column(nullable = false) private Instant occurredAt;

    static AuditLog from(AuditEvent event, AuditContextProvider.Context context, AuditSnapshots snapshots) {
        java.util.Objects.requireNonNull(event.action());
        java.util.Objects.requireNonNull(event.result());
        if (event.resourceType() == null || !event.resourceType().matches("[A-Z_]{1,64}"))
            throw new IllegalArgumentException("Invalid audit resource type");
        AuditLog log = new AuditLog();
        log.requestId = context.requestId(); log.traceId = context.traceId();
        log.actorId = context.actorId(); log.actorType = context.actorType();
        log.action = event.action(); log.resourceType = event.resourceType();
        log.resourceId = AuditContextProvider.safeText(event.resourceId(), 128);
        log.description = event.action().name();
        log.oldValue = snapshots.json(event.oldValue()); log.newValue = snapshots.json(event.newValue());
        log.ipAddress = context.ipAddress(); log.userAgent = context.userAgent();
        log.serviceName = context.serviceName(); log.result = event.result();
        log.errorCode = AuditContextProvider.safeText(event.errorCode(), 64);
        log.occurredAt = Instant.now();
        return log;
    }
}
