package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.NotificationOutbox;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.time.Instant;
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from NotificationOutbox e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt")
    List<NotificationOutbox> pending(Instant now, Pageable page);
}
