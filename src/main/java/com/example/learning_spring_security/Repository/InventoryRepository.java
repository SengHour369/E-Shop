package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Inventory;
import com.example.learning_spring_security.Model.ProductSku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductSkuId(Long productSkuId);

    boolean existsByProductSkuId(Long productSkuId);

    // Low stock – returns Inventory records (used for paginated view)
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= :threshold")
    Page<Inventory> findLowStock(@Param("threshold") Long threshold, Pageable pageable);

    // Inventory by product ID (paginated)
    @Query("SELECT i FROM Inventory i JOIN i.productSku sku JOIN sku.product p WHERE p.id = :productId")
    Page<Inventory> findByProductId(@Param("productId") Long productId, Pageable pageable);

    // ---------- Stock update methods (by productSkuId) ----------
    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity WHERE i.productSku.id = :productSkuId AND i.quantity >= :quantity")
    int reduceStock(@Param("productSkuId") Long productSkuId, @Param("quantity") Long quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity WHERE i.productSku.id = :productSkuId")
    int increaseStock(@Param("productSkuId") Long productSkuId, @Param("quantity") Long quantity);

    // ---------- Low stock SKU lists (return ProductSku directly) ----------
    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE i.quantity <= i.lowStockThreshold")
    List<ProductSku> findLowStockSkus();

    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE sku.product.id = :productId AND i.quantity <= i.lowStockThreshold")
    List<ProductSku> findLowStockSkusByProductId(@Param("productId") Long productId);

    // ---------- Default SKU for a product ----------
    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE sku.product.id = :productId AND i.isDefault = true")
    Optional<ProductSku> findDefaultSkuByProductId(@Param("productId") Long productId);
}