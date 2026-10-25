package com.example.eshop.catalog.repository;
import com.example.eshop.catalog.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.List;
public interface ProductReviewRepository extends JpaRepository<ProductReview,Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ProductReview> {
    List<ProductReview> findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(Long productId,String status);
    Page<ProductReview> findByModerationStatus(String status,Pageable page);
}
