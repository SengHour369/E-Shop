package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {

    public static Product toEntity(ProductRequest request, SubCategory subCategory, List<ProductSkuRequest> productSku) {

          Product  product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .subCategory(subCategory)
                .build();
        product.setProductSkus(productSku.stream()
                .map(skuRequest -> ProductSkuMapper.toEntity(skuRequest, product))
                .collect(Collectors.toList()));
       return product;
    }

    public static ResponseErrorTemplate toResponse(Product product) {
        ProductResponse productResponse  = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .Image(product.getImage().stream().map(Image::getUrl)
                        .collect(Collectors.toList()))
                .isActive(product.getIsActive())
                .skus(product.getProductSkus().stream()
                        .map(ProductSkuMapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, productResponse);
    }

    public static void updateEntity(Product product, ProductRequest request,
                                    SubCategory subCategory) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setIsActive(request.getIsActive());
        product.setSubCategory(subCategory);
    }
}