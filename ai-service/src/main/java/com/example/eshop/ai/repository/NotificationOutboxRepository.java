package com.example.eshop.ai.repository;

import com.example.eshop.ai.model.NotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from NotificationOutbox e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt")
    List<NotificationOutbox> pending(Instant now, Pageable page);
}
