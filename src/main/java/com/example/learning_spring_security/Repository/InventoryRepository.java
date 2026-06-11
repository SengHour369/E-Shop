package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductSkuId(Long productSkuId);

    boolean existsByProductSkuId(Long productSkuId);

    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= :threshold")
    Page<Inventory> findLowStock(@Param("threshold") Long threshold, Pageable pageable);

    @Query("SELECT i FROM Inventory i JOIN i.productSku sku JOIN sku.product p WHERE p.id = :productId")
    Page<Inventory> findByProductId(@Param("productId") Long productId, Pageable pageable);
}