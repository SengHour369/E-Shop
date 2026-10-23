package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.service.impl.AiProductSearchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-product-search;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ProductReadQueryTest.Config.class)
@Import(AiProductSearchService.class)
class AiProductSearchTest {
    @Autowired EntityManager em;
    @Autowired AiProductSearchService search;

    @Test void matchesAllWordsAndExcludesInactiveDeletedAndWildcardExpansion() {
        sku("Nike running shoes", "89.99", 4);
        sku("Other shoes", "49.00", 4);
        var hidden = sku("Nike shoes hidden", "20", 4); hidden.getProduct().setIsActive(false);
        var deleted = sku("Nike shoes deleted", "20", 4); deleted.getProduct().setDeleted(true);
        em.flush();
        assertThat(search.search("nike shoe", null, null, null)).extracting(c -> c.name()).containsExactly("Nike running shoes");
        assertThat(search.search("%", null, null, null)).isEmpty();
        assertThat(search.search("not-present", null, null, null)).isEmpty();
    }

    @Test void filtersEffectivePromotionPriceAndReportsLiveStockAndImage() {
        var phone = sku("Smartphone Pro", "600", 5);
        var image = new Image(); image.setProduct(phone.getProduct()); image.setUrl("https://example.test/phone.png"); em.persist(image);
        var promotion = new Promotion(); promotion.setName("Phone sale");
        promotion.setPromotionType(PromotionType.PRODUCT_DISCOUNT); promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("60")); promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setStartAt(LocalDateTime.now().minusDays(1)); promotion.setEndAt(LocalDateTime.now().plusDays(1)); em.persist(promotion);
        var assignment = new PromotionSku(); assignment.setProductSku(phone); assignment.setPromotion(promotion); em.persist(assignment);
        sku("Smartphone sold out", "250", 0);
        sku("Smartphone expensive", "900", 4);
        em.flush(); em.clear();
        var result = search.search("phone", new BigDecimal("200"), new BigDecimal("300"), true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).images()).containsExactly("https://example.test/phone.png");
        var card = result.get(0).skus().get(0);
        assertThat(card.id()).isEqualTo(phone.getId());
        assertThat(card.effectivePrice()).isEqualByComparingTo("240");
        assertThat(card.basePrice()).isEqualByComparingTo("600");
        assertThat(card.hasPromotion()).isTrue(); assertThat(card.promotionName()).isEqualTo("Phone sale");
        assertThat(card.available()).isEqualTo(5);
    }

    @Test void scansBeyondFirstCandidatePageAndCapsResults() {
        for (int i = 0; i < 101; i++) sku("Phone expensive " + i, "900", 1);
        for (int i = 0; i < 22; i++) sku("Phone budget " + i, "200", 1);
        em.flush();
        assertThat(search.search("phone", null, new BigDecimal("300"), null)).hasSize(20)
                .allSatisfy(card -> assertThat(card.name()).startsWith("Phone budget"));
    }

    @Test void searchesKhmerAndEnglishCatalogNames() {
        sku("ទូរស័ព្ទ Pro", "200", 1);
        sku("Smartphone Pro", "200", 1);
        em.flush();
        assertThat(search.search("phone", null, null, null)).hasSize(2);
        assertThat(search.search("ទូរស័ព្ទ", null, null, null)).hasSize(2);
    }

    @Test void searchesRealBrandAttributesInMixedKhmerEnglishQuery(){
        var shoe=sku("ស្បែកជើង running", "90", 3);shoe.setOperatorProductAttribute(true);
        var brand=new ProductAttribute();brand.setName("Brand");brand.setProductSkuId(shoe.getId());em.persist(brand);
        var value=new ProductAttributeValue();value.setAttributeId(brand.getId());value.setValue("Nike");em.persist(value);
        em.flush();em.clear();
        var results=search.search("Nike ស្បែកជើង",null,new BigDecimal("100"),true);
        assertThat(results).hasSize(1);assertThat(results.get(0).brand()).isEqualTo("Nike");
    }
    @Test void rejectsInvalidFiltersBeforeQuerying() {
        assertThatThrownBy(() -> search.search("phone", new BigDecimal("500"), new BigDecimal("300"), null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> search.search("phone", null, new BigDecimal("-1"), null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private ProductSku sku(String name, String price, long available) {
        var product = new Product(); product.setName(name); em.persist(product);
        var sku = new ProductSku(); sku.setProduct(product); sku.setSku(name); sku.setDescription(name);
        sku.setPrice(new BigDecimal(price)); sku.setIsDefault(true); em.persist(sku);
        var stock = new Inventory(); stock.setProductSku(sku); stock.setQuantity(available); stock.setAvailableQuantity(available);
        em.persist(stock);
        return sku;
    }
}
