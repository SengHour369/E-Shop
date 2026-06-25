package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Repository.ProductAttributeRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ProductAttributeService;
import com.example.learning_spring_security.ServiceMapper.ProductAttributeMapper;
import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import com.example.learning_spring_security.dto.Response.ProductSkuResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
            throw new DuplicateResourceException("Attribute already exists with name: " + request.getName());
        }

        ProductAttributeMapper.updateEntity(attribute, request.getName());
        ProductAttribute updatedAttribute = productAttributeRepository.save(attribute);

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest valueRequest : request.getAttributes()) {
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


