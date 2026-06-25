package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.ProductAttributeRepository;
import com.example.learning_spring_security.Repository.ProductAttributeValueRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import com.example.learning_spring_security.dto.Response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final ProductSkuRepository skuRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository valueRepository;

    public static Product toEntity(ProductRequest request, SubCategory subCategory) {

        Product  product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .subCategory(subCategory)
                .build();

        return product;
    }


    public ResponseErrorTemplate toResponse(Product product) {

        List<ProductSkuResponse> skuResponses =
                skuRepository.findByProductId(product.getId())
                        .stream()
                        .map(this::toSkuResponse)
                        .toList();

        ProductResponse p =  ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .skus(skuResponses)
                .Image(product.getImage().stream().map(Image::getUrl)
                        .collect(Collectors.toList()))
                .build();

        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, p);
    }

    public ProductSkuResponse toSkuResponse(ProductSku sku) {

        List<ProductAttributeResponse> attributeResponses =
                attributeRepository.findByProductSkuId(sku.getId())
                        .stream()
                        .map(this::toAttributeResponse)
                        .toList();

        return ProductSkuResponse.builder()
                .id(sku.getId())
                .sku(sku.getSku())
                .description(sku.getDescription())
                .price(sku.getPrice())
                .quantity(sku.getQuantity())
                .isDefault(sku.getIsDefault())
                .OperatorProductAttribute(sku.getOperatorProductAttribute())
                .ProductAttributeResponse(attributeResponses)
                .build();
    }

    public ProductAttributeResponse toAttributeResponse(ProductAttribute attribute) {

        List<ProductAttributeValueResponse> valueResponses =
                valueRepository.findByAttributeId(attribute.getId())
                        .stream()
                        .map(v -> ProductAttributeValueResponse.builder()
                                .id(v.getId())
                                .value(v.getValue())
                                .build())
                        .toList();

        return ProductAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .attributes(valueResponses)
                .build();
    }

    public static void updateEntity(Product product, ProductRequest request,
                                    SubCategory subCategory) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setIsActive(request.getIsActive());
        product.setSubCategory(subCategory);

    }
}