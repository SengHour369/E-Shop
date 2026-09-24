package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    @Query("SELECT pa FROM ProductAttribute pa WHERE LOWER(pa.name) = LOWER(:name)")
    Optional<ProductAttribute> findByNameIgnoreCase(@Param("name") String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT pa FROM ProductAttribute pa ORDER BY pa.name ASC")
    List<ProductAttribute> findAllOrderByName();
    List<ProductAttribute> findByProductSkuId(Long productSkuId);
}

