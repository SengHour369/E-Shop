package com.example.eshop.catalog.seed;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeedAssetConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/category-icons/seed/**")
                .addResourceLocations("classpath:/seed-assets/categories/").setCachePeriod(86400);
        registry.addResourceHandler("/api/v1/brand-logos/seed/**")
                .addResourceLocations("classpath:/seed-assets/brands/").setCachePeriod(86400);
    }
}
