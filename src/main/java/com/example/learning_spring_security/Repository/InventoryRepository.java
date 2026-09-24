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

    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= :threshold")
    Page<Inventory> findLowStock(@Param("threshold") Long threshold, Pageable pageable);

    @Query("SELECT i FROM Inventory i JOIN i.productSku sku JOIN sku.product p WHERE p.id = :productId")
    Page<Inventory> findByProductId(@Param("productId") Long productId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity WHERE i.productSku.id = :productSkuId AND i.quantity >= :quantity")
    int reduceStock(@Param("productSkuId") Long productSkuId, @Param("quantity") Long quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity WHERE i.productSku.id = :productSkuId")
    int increaseStock(@Param("productSkuId") Long productSkuId, @Param("quantity") Long quantity);

    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE i.quantity <= i.lowStockThreshold")
    List<ProductSku> findLowStockSkus();

    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE sku.product.id = :productId AND i.quantity <= i.lowStockThreshold")
    List<ProductSku> findLowStockSkusByProductId(@Param("productId") Long productId);

    @Query("SELECT sku FROM Inventory i JOIN i.productSku sku WHERE sku.product.id = :productId AND i.isDefault = true")
    Optional<ProductSku> findDefaultSkuByProductId(@Param("productId") Long productId);

    // ---------- Dashboard summary ----------
    @Query("SELECT COUNT(DISTINCT p.id) FROM Product p WHERE EXISTS (" +
            "SELECT 1 FROM ProductSku s WHERE s.product = p AND EXISTS (" +
            "SELECT 1 FROM Inventory i WHERE i.productSku = s))")
    Long countDistinctProductsWithInventory();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i")
    Long sumTotalStock();

    @Query("SELECT COALESCE(COUNT(i), 0) FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.lowStockThreshold")
    Long countLowStock();

    @Query("SELECT COALESCE(COUNT(i), 0) FROM Inventory i WHERE (i.quantity - i.reservedQuantity) = 0")
    Long countOutOfStock();

    // ---------- Search (without barcode for now) ----------
    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.productSku sku " +
            "JOIN FETCH sku.product p " +
            "WHERE (:search IS NULL OR " +
            "       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "       LOWER(sku.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:warehouse IS NULL OR i.warehouseLocation = :warehouse) " +
            "AND (:status IS NULL OR " +
            "     (CASE WHEN :status = 'IN_STOCK' THEN (i.quantity - i.reservedQuantity) > i.lowStockThreshold " +
            "           WHEN :status = 'LOW_STOCK' THEN (i.quantity - i.reservedQuantity) <= i.lowStockThreshold " +
            "                                         AND (i.quantity - i.reservedQuantity) > 0 " +
            "           WHEN :status = 'OUT_OF_STOCK' THEN (i.quantity - i.reservedQuantity) = 0 " +
            "           ELSE TRUE END))")
    Page<Inventory> searchInventory(@Param("search") String search,
                                    @Param("warehouse") String warehouse,
                                    @Param("status") String status,
                                    Pageable pageable);
}