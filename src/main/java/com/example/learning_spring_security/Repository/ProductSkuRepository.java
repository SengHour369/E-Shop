package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findBySku(String sku);

    List<ProductSku> findByProductId(Long productId);

    boolean existsBySku(String sku);

    @Modifying
    @Transactional
    @Query("UPDATE ProductSku s SET s.quantity = s.quantity - :quantity WHERE s.id = :skuId AND s.quantity >= :quantity")
    int reduceStock(@Param("skuId") Long skuId, @Param("quantity") Long quantity);

    @Modifying
    @Transactional
    @Query("UPDATE ProductSku s SET s.quantity = s.quantity + :quantity WHERE s.id = :skuId")
    int increaseStock(@Param("skuId") Long skuId, @Param("quantity") Long quantity);

    @Query("SELECT s FROM ProductSku s WHERE s.product.id = :productId AND s.isDefault = true")
    Optional<ProductSku> findDefaultSkuByProductId(@Param("productId") Long productId);

    @Query("SELECT s FROM ProductSku s WHERE s.quantity <= s.lowStockThreshold")
    List<ProductSku> findLowStockSkus();

    @Query("SELECT s FROM ProductSku s WHERE s.product.id = :productId AND s.quantity <= s.lowStockThreshold")
    List<ProductSku> findLowStockSkusByProductId(@Param("productId") Long productId);
}