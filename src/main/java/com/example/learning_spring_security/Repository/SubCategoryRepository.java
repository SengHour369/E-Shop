package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.SubCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {


    Page<SubCategory> findAll(Pageable pageable);

    List<SubCategory> findByCategoryId(Long categoryId);

    @Query("SELECT s FROM SubCategory s LEFT JOIN FETCH s.products WHERE s.id = :id")
    Optional<SubCategory> findByIdWithProducts(@Param("id") Long id);

    boolean existsByNameAndCategoryId(String name, Long categoryId);
    @Query("SELECT s FROM SubCategory s JOIN FETCH s.category WHERE s.category.id = :categoryId")
    List<SubCategory> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);
}