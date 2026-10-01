package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    long countByPromotionIdAndReleasedFalse(Long promotionId);
    long countByPromotionIdAndUserIdAndReleasedFalse(Long promotionId, Long userId);
    List<PromotionUsage> findByOrderId(Long orderId);
}

