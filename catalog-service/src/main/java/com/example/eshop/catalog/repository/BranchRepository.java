package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.Branch;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
public interface BranchRepository extends JpaRepository<Branch,Long> {
 boolean existsByNameKey(String key);
 @Query("select b from Branch b where b.active=true and exists(select a.id from BranchAccess a where a.branchId=b.id and a.userId=:actor and a.active=true)")
 Page<Branch> visible(@Param("actor") long actor,Pageable pageable);
}
