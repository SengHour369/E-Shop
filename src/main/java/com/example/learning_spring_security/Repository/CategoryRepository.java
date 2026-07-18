package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT c
        FROM Category c
        WHERE LOWER(c.name) = LOWER(:name)
        AND (c.deleted IS NULL OR c.deleted = false)
    """)
    Optional<Category> findByName(@Param("name") String name);

    boolean existsByNameAndDeletedFalse(String name);

    @Query("""
        SELECT c
        FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE c.id = :id
        AND (c.deleted IS NULL OR c.deleted = false)
    """)
    Optional<Category> findByIdWithSubCategories(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE (c.deleted IS NULL OR c.deleted = false)
    """)
    List<Category> findAllWithSubCategories();

    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
        SELECT c
        FROM Category c
        WHERE c.id = :id
        AND (c.deleted IS NULL OR c.deleted = false)
    """)
    Optional<Category> findByCategoryId(@Param("id") Long id);
    @Query("SELECT c FROM Category c WHERE c.deleted IS NULL OR c.deleted = false")
    List<Category> findAllActive();

}