package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.BranchAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BranchAccessRepository extends JpaRepository<BranchAccess,UUID> {
 boolean existsByBranchIdAndUserIdAndActiveTrue(Long branchId,Long userId);
 Optional<BranchAccess> findByBranchIdAndUserId(Long branchId,Long userId);
 java.util.List<BranchAccess> findByBranchId(Long branchId);
}
