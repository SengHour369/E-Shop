package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

    @Query("""
        SELECT pav 
        FROM ProductAttributeValue pav 
        WHERE pav.attributeId = :attributeId 
        AND LOWER(pav.value) = LOWER(:value)
    """)
    Optional<ProductAttributeValue> findByAttributeIdAndValueIgnoreCase(
            @Param("attributeId") Long attributeId,
            @Param("value") String value
    );

    @Query("""
        SELECT pav 
        FROM ProductAttributeValue pav 
        WHERE pav.attributeId = :attributeId 
        ORDER BY pav.value ASC
    """)
    List<ProductAttributeValue> findByAttributeId(@Param("attributeId") Long attributeId);

    boolean existsByAttributeIdAndValue(Long attributeId, String value);
}