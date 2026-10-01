package com.example.eshop.catalog.service.impl;
import com.example.eshop.catalog.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import com.example.eshop.common.audit.*;
import static com.example.eshop.catalog.audit.CatalogAuditSnapshots.of;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service @RequiredArgsConstructor
public class PromotionLifecycleService {
    private final AuditLogService audit;
    private final PromotionRepository promotions;
    private final Clock clock;
    @Scheduled(fixedDelayString = "${promotions.sync-delay-ms:30000}")
    @Transactional
    public void synchronize() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (var promotion : promotions.due(now, org.springframework.data.domain.PageRequest.of(0, 200))) {
            var before = of(promotion);
            boolean expired = !promotion.getEndAt().isAfter(now);
            promotion.setStatus(expired ? com.example.eshop.catalog.enumeration.PromotionStatus.EXPIRED
                    : com.example.eshop.catalog.enumeration.PromotionStatus.ACTIVE);
            audit.record(AuditEvent.success(expired ? AuditAction.PROMOTION_EXPIRE : AuditAction.PROMOTION_ACTIVATE,
                    "PROMOTION", promotion.getId(), before, of(promotion)));
        }
    }
}

