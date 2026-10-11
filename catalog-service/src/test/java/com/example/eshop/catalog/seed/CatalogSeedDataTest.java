package com.example.eshop.catalog.seed;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashSet;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogSeedDataTest {
    @Test
    void providesOneHundredUniqueCategoriesAndBrandsWithValidBundledImages() throws Exception {
        var categories = CatalogSeedData.categories();
        var brands = CatalogSeedData.brands();
        assertThat(categories).hasSize(100);
        assertThat(brands).hasSize(100);
        assertThat(new HashSet<>(categories.stream().map(CatalogSeedData.CategorySeed::name).toList())).hasSize(100);
        assertThat(new HashSet<>(brands.stream().map(CatalogSeedData.BrandSeed::name).toList())).hasSize(100);
        for (var category : categories) validateSvg("categories/" + category.slug());
        for (var brand : brands) validateSvg("brands/" + brand.slug());
    }
    private void validateSvg(String path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var input = new ClassPathResource("seed-assets/" + path + ".svg").getInputStream()) {
            var document = factory.newDocumentBuilder().parse(input);
            assertThat(document.getDocumentElement().getTagName()).isEqualTo("svg");
            assertThat(document.getElementsByTagName("script").getLength()).isZero();
        }
    }
}
