package com.example.eshop.catalog.repository;

import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.isActive = true and (p.deleted = false or p.deleted is null) and exists (select ps.id from PromotionSku ps where ps.promotion.id = :promotionId and ps.productSku.product = p)")
    org.springframework.data.domain.Page<Product> findPromotionProducts(
        @Param("promotionId") Long promotionId, org.springframework.data.domain.Pageable pageable);
    // Fetch a collection only after pagination, never in the pageable query.
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.image WHERE p.id IN :ids")
    List<Product> fetchImages(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (p.deleted IS NULL OR p.deleted = false)")
    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.deleted IS NULL OR p.deleted = false)")
    Page<Product> findAllNotDeleted(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND (p.deleted IS NULL OR p.deleted = false)")
    Optional<Product> findByIdNotDeleted(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.subCategory.id = :subCategoryId AND (p.deleted IS NULL OR p.deleted = false)")
    Page<Product> findBySubCategoryId(@Param("subCategoryId") Long subCategoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (p.deleted IS NULL OR p.deleted = false)")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productSkus WHERE p.id = :id AND (p.deleted IS NULL OR p.deleted = false)")
    Optional<Product> findByIdWithSkus(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.subCategory.category.id = :categoryId AND (p.deleted IS NULL OR p.deleted = false)")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.subCategory.id = :subCategoryId AND (p.deleted IS NULL OR p.deleted = false)")
    Long countBySubCategoryId(@Param("subCategoryId") Long subCategoryId);

}
