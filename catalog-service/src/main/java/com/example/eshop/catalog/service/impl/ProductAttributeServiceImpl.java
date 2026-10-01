package com.example.eshop.catalog.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.ProductAttribute;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.ProductAttributeRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.ProductAttributeService;
import com.example.eshop.catalog.mapper.ProductAttributeMapper;
import com.example.eshop.catalog.dto.request.ProductAttributeRequest;
import com.example.eshop.catalog.dto.request.ProductAttributeValueRequest;
import com.example.eshop.catalog.dto.response.ProductAttributeResponse;
import com.example.eshop.catalog.dto.response.ProductSkuResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueServiceImpl productAttributeValueServiceImpl;
    private final ProductSkuRepository productSkuRepository;

    @Override
    public ProductAttributeResponse createAttribute(Long id, ProductAttributeRequest request) {
        ProductSku productSku = productSkuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found with id: " + id));

        ProductAttribute attribute = ProductAttributeMapper.toEntity(request.getName(),productSku);
        ProductAttribute savedAttribute = productAttributeRepository.save(attribute);
        if (request.getAttributes() != null ) {
            for( ProductAttributeValueRequest productAttributeValueRequest : request.getAttributes()){
                this.productAttributeValueServiceImpl.createAttributeValue(savedAttribute.getId(),productAttributeValueRequest);
            }
        }
        return ProductAttributeMapper.toResponse(savedAttribute);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeResponse getAttributeById(Long id) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));
        return ProductAttributeMapper.toResponse(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeResponse getAttributeByName(String name) {
        ProductAttribute attribute = productAttributeRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with name: " + name));
        return ProductAttributeMapper.toResponse(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeResponse> getAllAttributes() {
        List<ProductAttribute> attributes = productAttributeRepository.findAllOrderByName();
        return attributes.stream()
                .map(ProductAttributeMapper::toResponse)
                .toList();
    }

    @Override
    public ProductAttributeResponse updateAttribute(Long id, ProductAttributeRequest request) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));

        if (!attribute.getName().equalsIgnoreCase(request.getName()) && productAttributeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessLogicException("Attribute already exists with name: " + request.getName());
        }

        ProductAttributeMapper.updateEntity(attribute, request.getName());
        ProductAttribute updatedAttribute = productAttributeRepository.save(attribute);

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (com.example.eshop.catalog.dto.request.ProductAttributeValueRequest valueRequest : request.getAttributes()) {
                if (valueRequest.getId() != null) {
                    productAttributeValueServiceImpl.updateAttributeValue(valueRequest.getId(), valueRequest);
                } else {
                    productAttributeValueServiceImpl.createAttributeValue(updatedAttribute.getId(), valueRequest);
                }
            }
        }

        return ProductAttributeMapper.toResponse(updatedAttribute);
    }



    @Override
    public void deleteAttribute(Long id) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));
        productAttributeRepository.delete(attribute);
    }
}
