package com.example.eshop.common.audit;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Deliberately exposes no update or delete operations. */
public interface AuditLogRepository extends Repository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    AuditLog save(AuditLog entry);
}
