package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.controller.AiCatalogController.ProductCard;
import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.repository.ProductRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search public products, then filter using the same live promotion pricing as the storefront. */
@Service
@RequiredArgsConstructor
public class AiProductSearchService {
    private final ProductRepository products;
    private final ProductResponseService responses;

    @Transactional(readOnly = true)
    public List<ProductCard> search(String query, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {
        if (query == null || query.isBlank() || query.length() > 120
                || query.chars().anyMatch(Character::isISOControl)
                || !validPrice(minPrice) || !validPrice(maxPrice)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product search filters.");
        }
        var found = new ArrayList<ProductCard>();
        // Do not filter base prices in SQL: an active promotion may bring a SKU into the requested range.
        for (int page = 0; ; page++) {
            var candidates = products.findAll(matching(query), PageRequest.of(page, 100, Sort.by("id")));
            var pageProducts=responses.toProductResponses(candidates.getContent());
            var categories=responses.aiCategoryNames(pageProducts.stream().map(com.example.eshop.catalog.dto.response.ProductResponse::getId).toList());
            for (var product : pageProducts) {
                var card = ProductCard.from(product,categories.get(product.getId()));
                var matchingSkus = card.skus().stream().filter(sku -> {
                    var price = sku.effectivePrice() == null ? sku.basePrice() : sku.effectivePrice();
                    return price != null && price.signum() >= 0
                            && (minPrice == null || price.compareTo(minPrice) >= 0)
                            && (maxPrice == null || price.compareTo(maxPrice) <= 0)
                            && (!Boolean.TRUE.equals(inStock) || (sku.available() != null && sku.available() > 0));
                }).toList();
                if (!matchingSkus.isEmpty()) {
                    found.add(card.withSkus(matchingSkus));
                    if (found.size() == 20) return List.copyOf(found);
                }
            }
            if (!candidates.hasNext()) return List.copyOf(found);
        }
    }

    private static boolean validPrice(BigDecimal value) {
        return value == null || (value.signum() >= 0 && value.compareTo(new BigDecimal("1000000")) <= 0
                && value.stripTrailingZeros().scale() <= 2);
    }

    public static Specification<Product> matching(String query) {
        return (root, criteria, cb) -> {
            var conditions = new ArrayList<jakarta.persistence.criteria.Predicate>();
            conditions.add(cb.isTrue(root.get("isActive")));
            conditions.add(cb.or(cb.isFalse(root.get("deleted")), cb.isNull(root.get("deleted"))));
            var category = root.join("subCategory", JoinType.LEFT);
            var parent = category.join("category", JoinType.LEFT);
            for (var term : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
                var alternatives = new ArrayList<jakarta.persistence.criteria.Predicate>();
                for (String keyword : keywords(term)) {
                    String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                    String pattern = "%" + escaped + "%";
                    var attributes=criteria.subquery(Long.class);
                    var attribute=attributes.from(com.example.eshop.catalog.model.ProductAttribute.class);
                    var value=attributes.from(com.example.eshop.catalog.model.ProductAttributeValue.class);
                    var sku=attributes.from(com.example.eshop.catalog.model.ProductSku.class);
                    attributes.select(cb.literal(1L)).where(cb.equal(sku.get("product"),root),cb.equal(attribute.get("productSkuId"),sku.get("id")),
                        cb.equal(value.get("attributeId"),attribute.get("id")),cb.like(cb.lower(value.get("value")),pattern,'\\'));
                    alternatives.add(cb.or(cb.exists(attributes),cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(category.get("name")), pattern, '\\'),
                        cb.like(cb.lower(parent.get("name")), pattern, '\\')));
                }
                conditions.add(cb.or(alternatives.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static List<String> keywords(String term) {
        return switch (term) {
            case "phone", "phones", "ទូរស័ព្ទ" -> List.of("phone", "ទូរស័ព្ទ");
            case "smartphone", "smartphones", "ស្មាតហ្វូន" -> List.of("smartphone", "ស្មាតហ្វូន");
            case "shoe", "shoes", "ស្បែកជើង" -> List.of("shoe", "ស្បែកជើង");
            case "laptop", "laptops", "កុំព្យូទ័រយួរដៃ" -> List.of("laptop", "កុំព្យូទ័រយួរដៃ");
            default -> List.of(term);
        };
    }
}
