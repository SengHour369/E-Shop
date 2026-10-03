package com.example.eshop.notification.repository;
import com.example.eshop.notification.model.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,Long>, JpaSpecificationExecutor<Notification> {
    boolean existsByEventId(UUID eventId);
    Optional<Notification> findByIdAndUserId(Long id, long userId);
    long countByUserIdAndReadAtIsNull(long userId);
    @Modifying @Query("update Notification n set n.readAt=:now,n.status=com.example.eshop.notification.model.Notification.Status.READ where n.userId=:userId and n.readAt is null")
    int markAll(long userId, Instant now);
    @Modifying @Query("update Notification n set n.readAt=:now,n.status=com.example.eshop.notification.model.Notification.Status.READ where n.id=:id and n.userId=:userId and n.readAt is null")
    int markRead(long id, long userId, Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.emailStatus=com.example.eshop.notification.model.Notification.EmailStatus.PENDING and n.nextEmailAttemptAt<=:now order by n.createdAt")
    List<Notification> emailDue(Instant now, Pageable page);
}
