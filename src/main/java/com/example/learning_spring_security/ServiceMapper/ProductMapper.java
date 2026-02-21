package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import java.util.stream.Collectors;

public class ProductMapper {

    public static Product toEntity(ProductRequest request, SubCategory subCategory) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
//                .image(request.getImage())
//                .mainImage(request.getMainImage())
                .isActive(request.getIsActive())
                .subCategory(subCategory)
                .build();
    }

    public static ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
//                .image(product.getImage())
                .mainImage(product.getMainImage())
                .isActive(product.getIsActive())
                .subCategory(SubCategoryMapper.toResponse(product.getSubCategory()))
                .skus(product.getProductSkus().stream()
                        .map(ProductSkuMapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public static void updateEntity(Product product, ProductRequest request,
                                    SubCategory subCategory) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
//        product.setImage(request.getImage());
//        product.setMainImage(request.getMainImage());
        product.setIsActive(request.getIsActive());
        product.setSubCategory(subCategory);
    }
}