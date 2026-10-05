package com.example.eshop.catalog.repository;

import com.example.eshop.catalog.model.ProductAttribute;
import com.example.eshop.catalog.model.ProductSku;
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
    interface SkuInventory {
        ProductSku getSku();
        com.example.eshop.catalog.model.Inventory getInventory();
    }
    @Query("SELECT s AS sku, i AS inventory FROM ProductSku s JOIN FETCH s.product LEFT JOIN FETCH s.image LEFT JOIN Inventory i ON i.productSku = s WHERE s.product.id IN :ids ORDER BY s.id")
    List<SkuInventory> findWithInventoryForProducts(@Param("ids") List<Long> ids);

    Optional<ProductSku> findBySku(String sku);

    List<ProductSku> findByProductId(Long productId);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    @Query("""
            select distinct s from ProductSku s
            join fetch s.product
            left join fetch s.image
            where s.barcode in :codes
            """)
    List<ProductSku> findByBarcodes(@Param("codes") java.util.Collection<String> codes);

    @Query("""
            select distinct s from ProductSku s
            join fetch s.product
            left join fetch s.image
            where s.sku in :codes
            """)
    List<ProductSku> findBySkuCodes(@Param("codes") java.util.Collection<String> codes);

    @Query("""
            select s from ProductSku s
            left join fetch s.image
            where s.product.id in :ids
            order by s.product.id, s.id
            """)
    List<ProductSku> findForScannerProducts(@Param("ids") java.util.Collection<Long> ids);



}
