package com.example.eshop.catalog.scanner;

import com.example.eshop.catalog.dto.response.ImageCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogImageMatcherTest {
    private static CatalogImageMatcher.ProductMatch product(long id, String name) {
        return new CatalogImageMatcher.ProductMatch(id, id + 400, name, "SKU-" + id, "https://cdn.example/" + id);
    }

    @Test void highConfidenceExactNameCanBeSuggested() {
        var response = CatalogImageMatcher.match(List.of(new VisionLabel("Nike Air Max", 0.95)),
                name -> List.of(product(100, "Nike Air Max")), "req_ai");
        assertThat(response.getMatchType()).isEqualTo("AI_IMAGE");
        assertThat(response.isExact()).isTrue();
        assertThat(response.isRequiresSelection()).isFalse();
        assertThat(response.getCandidates()).extracting(ImageCandidate::getProductId).containsExactly(100L);
    }

    @Test void lowConfidenceStaysACandidate() {
        var response = CatalogImageMatcher.match(List.of(new VisionLabel("Nike Air Max", 0.74)),
                name -> List.of(product(100, "Nike Air Max")), "req_ai");
        assertThat(response.isExact()).isFalse();
        assertThat(response.isRequiresSelection()).isTrue();
        assertThat(response.getCandidates()).hasSize(1);
        assertThat(response.getConfidence()).isEqualTo(0.74);
    }

    @Test void multipleCatalogMatchesRequireAChoice() {
        var response = CatalogImageMatcher.match(List.of(new VisionLabel("Nike Air Max", 0.91)),
                name -> List.of(product(100, "Nike Air Max"), product(102, "Nike Air Max 90")), "req_ai");
        assertThat(response.isExact()).isFalse();
        assertThat(response.getCandidates()).extracting(ImageCandidate::getName)
                .containsExactly("Nike Air Max", "Nike Air Max 90");
    }

    @Test void namesThatAreNotInTheCatalogProduceNoProduct() {
        var response = CatalogImageMatcher.match(List.of(new VisionLabel("Unknown Sneaker", 0.99)),
                name -> List.of(), "req_ai");
        assertThat(response.getMatchType()).isEqualTo("NO_CATALOG_MATCH");
        assertThat(response.getCandidates()).isEmpty();
        assertThat(response.getMessage()).isEqualTo("No matching catalog product");
    }

    @Test void unavailableResultInventsNothing() {
        var response = CatalogImageMatcher.unavailable("req_down");
        assertThat(response.getMatchType()).isEqualTo("AI_UNAVAILABLE");
        assertThat(response.getCandidates()).isEmpty();
        assertThat(response.isExact()).isFalse();
    }
}
