package com.example.eshop.catalog.reviews;

import com.example.eshop.catalog.model.ProductReview;
import com.example.eshop.catalog.repository.ProductReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql=false,properties={
 "spring.datasource.url=jdbc:h2:mem:guest-review-moderation;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop",
 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes=ReviewModerationIntegrationTest.Config.class)
class ReviewModerationIntegrationTest {
 @Configuration @EntityScan("com.example.eshop.catalog.model") @EnableJpaRepositories("com.example.eshop.catalog.repository") static class Config { }
 @Autowired ProductReviewRepository reviews;
 @Autowired com.example.eshop.catalog.repository.ProductRepository products;
 @Test void moderationFiltersSearchProductsAndCountEveryStatus() {
  var product=new com.example.eshop.catalog.model.Product();product.setName("Trail Shoe");product=products.saveAndFlush(product);
  var pending=draft();pending.setProductId(product.getId());reviews.saveAndFlush(pending);
  var approved=draft();approved.setProductId(product.getId());approved.setModerationStatus("APPROVED");reviews.saveAndFlush(approved);
  var rejected=draft();rejected.setProductId(product.getId());rejected.setModerationStatus("REJECTED");rejected.setRating(2);reviews.saveAndFlush(rejected);
  var controller=new com.example.eshop.catalog.controller.ProductReviewController(reviews,org.mockito.Mockito.mock(com.example.eshop.catalog.repository.ProductSkuRepository.class),products,org.mockito.Mockito.mock(com.example.eshop.common.storefront.StorefrontAccessClient.class),org.mockito.Mockito.mock(com.example.eshop.common.security.LivePermissionService.class),org.mockito.Mockito.mock(com.example.eshop.common.audit.AuditLogService.class));
  var result=(java.util.Map<?,?>)controller.pending(0,"APPROVED","trail",5,"GUEST",null,null);
  assertThat((java.util.List<?>)result.get("content")).hasSize(1);
  var counts=(java.util.Map<?,?>)result.get("counts");
  assertThat(counts.get("ALL")).isEqualTo(2L);assertThat(counts.get("PENDING")).isEqualTo(1L);assertThat(counts.get("REJECTED")).isEqualTo(0L);
  var customer=(java.util.Map<?,?>)controller.pending(0,"ALL","",null,"CUSTOMER",null,null);
  assertThat((java.util.List<?>)customer.get("content")).isEmpty();
  assertThatThrownBy(()->controller.pending(0,"ALL","",6,"",null,null)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }

 ProductReview draft() {var review=new ProductReview();review.setProductId(7L);review.setGuestSessionId(UUID.randomUUID());review.setAuthor("Guest");review.setRating(5);review.setTitle("Good product");review.setBody("Works well.");return review;}
 @Test void pendingAndRejectedGuestReviewsNeverAppearInPublicResults() {
  var pending=reviews.saveAndFlush(draft());
  assertThat(reviews.findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(7L,"APPROVED")).isEmpty();
  pending.setModerationStatus("REJECTED");reviews.saveAndFlush(pending);
  assertThat(reviews.findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(7L,"APPROVED")).isEmpty();
 }
 @Test void approvalPublishesOnlyApprovedReviewAndDoesNotInferPurchase() {
  reviews.saveAndFlush(draft());var approved=reviews.saveAndFlush(draft());approved.setModerationStatus("APPROVED");reviews.saveAndFlush(approved);
  var visible=reviews.findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(7L,"APPROVED");
  assertThat(visible).hasSize(1);assertThat(visible.get(0).isVerifiedPurchase()).isFalse();
 }
 @Test void uniqueGuestProductReviewPreventsRepeatedSubmissions() {
  var first=reviews.saveAndFlush(draft());var duplicate=draft();duplicate.setGuestSessionId(first.getGuestSessionId());
  assertThatThrownBy(()->reviews.saveAndFlush(duplicate)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
 }
}
