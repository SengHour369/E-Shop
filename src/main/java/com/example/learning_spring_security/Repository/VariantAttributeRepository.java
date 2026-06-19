package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.VariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, Long> {

    @Query("SELECT va FROM VariantAttribute va WHERE va.productSku.id = :skuId ORDER BY va.attribute.name ASC")
    List<VariantAttribute> findByProductSkuId(@Param("skuId") Long skuId);

    @Query("SELECT va FROM VariantAttribute va WHERE va.productSku.id = :skuId AND va.attribute.id = :attributeId")
    Optional<VariantAttribute> findByProductSkuIdAndAttributeId(@Param("skuId") Long skuId, @Param("attributeId") Long attributeId);

    boolean existsByProductSkuIdAndAttributeId(Long skuId, Long attributeId);

    @Query("SELECT va FROM VariantAttribute va WHERE va.productSku.id = :skuId AND va.attributeValue.id = :valueId")
    Optional<VariantAttribute> findByProductSkuIdAndAttributeValueId(@Param("skuId") Long skuId, @Param("valueId") Long valueId);
}

