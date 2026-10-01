package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.Promotion;
import com.example.eshop.catalog.enumeration.PromotionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where (p.status in (com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE, com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED) and p.endAt <= :now) or (p.isActive = true and p.status = com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED and p.startAt <= :now) order by p.id")
    List<Promotion> due(@Param("now") LocalDateTime now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> lockById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id in :ids order by p.id")
    List<Promotion> lockAll(@Param("ids") Collection<Long> ids);
    @Query("select p from Promotion p where (:status is null or p.status = :status)")
    Page<Promotion> search(@Param("status") PromotionStatus status, Pageable pageable);
    @Modifying
    @Query("update Promotion p set p.status = com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE where p.isActive = true and p.status = com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED and p.startAt <= :now and p.endAt > :now")
    int activateDue(@Param("now") LocalDateTime now);
    @Modifying
    @Query("update Promotion p set p.status = com.example.eshop.catalog.enumeration.PromotionStatus.EXPIRED where p.status in (com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE, com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED) and p.endAt <= :now")
    int expireDue(@Param("now") LocalDateTime now);
    @Query("select p from Promotion p where p.isActive = true and p.status in (com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE, com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED) and p.startAt <= :now and p.endAt > :now and (p.usagePerCustomer is null or p.usagePerCustomer > 0) and (p.usageLimit is null or p.usageLimit > (select count(u) from PromotionUsage u where u.promotion = p and u.released = false))")
    Page<Promotion> active(@Param("now") LocalDateTime now, Pageable pageable);
}

