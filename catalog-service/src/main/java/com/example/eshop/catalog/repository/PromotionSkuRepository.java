package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import java.util.*;
public interface PromotionSkuRepository extends JpaRepository<PromotionSku, Long> {
    @Query("select ps from PromotionSku ps join fetch ps.promotion p where ps.productSku.id in :ids and p.isActive = true and p.status in (com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE, com.example.eshop.catalog.enumeration.PromotionStatus.SCHEDULED) and p.startAt <= :now and p.endAt > :now and (p.usagePerCustomer is null or p.usagePerCustomer > 0) and (p.usageLimit is null or p.usageLimit > (select count(u) from PromotionUsage u where u.promotion = p and u.released = false))")
    List<PromotionSku> eligible(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);
    boolean existsByPromotionIdAndProductSkuId(Long promotionId, Long skuId);
    void deleteByPromotionIdAndProductSkuId(Long promotionId, Long skuId);
    @Query("select ps.productSku.id from PromotionSku ps where ps.promotion.id = :id")
    Page<Long> skuIds(@Param("id") Long id, Pageable pageable);
}

