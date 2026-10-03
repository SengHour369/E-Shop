package com.example.eshop.catalog.repository;

import com.example.eshop.catalog.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    interface AttributeValue {
        ProductAttribute getAttribute();
        com.example.eshop.catalog.model.ProductAttributeValue getAttributeValue();
    }
    @Query("SELECT a AS attribute, v AS attributeValue FROM ProductAttribute a LEFT JOIN ProductAttributeValue v ON v.attributeId = a.id WHERE a.productSkuId IN :skuIds ORDER BY a.id, v.value, v.id")
    List<AttributeValue> findWithValuesForSkus(@Param("skuIds") List<Long> skuIds);

    @Query("SELECT pa FROM ProductAttribute pa WHERE LOWER(pa.name) = LOWER(:name)")
    Optional<ProductAttribute> findByNameIgnoreCase(@Param("name") String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT pa FROM ProductAttribute pa ORDER BY pa.name ASC")
    List<ProductAttribute> findAllOrderByName();
    List<ProductAttribute> findByProductSkuId(Long productSkuId);
}

