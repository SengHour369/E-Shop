package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiDailyQuota;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface AiDailyQuotaRepository extends JpaRepository<AiDailyQuota,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select q from AiDailyQuota q where q.userId=:id")
 Optional<AiDailyQuota> lock(long id);
}
