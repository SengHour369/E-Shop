package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.Model.ProductAttributeValue;
import com.example.learning_spring_security.Repository.ProductAttributeRepository;
import com.example.learning_spring_security.Repository.ProductAttributeValueRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ProductAttributeValueService;
import com.example.learning_spring_security.ServiceMapper.ProductAttributeValueMapper;
import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAttributeValueServiceImpl implements ProductAttributeValueService {

    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductAttributeRepository productAttributeRepository;

    @Override
    public ProductAttributeValueResponse createAttributeValue(Long id, ProductAttributeValueRequest request) {
        ProductAttribute attribute = productAttributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + id));
        ProductAttributeValue attributeValue = ProductAttributeValue.builder()
                .attributeId(attribute.getId())
                .value(request.getValue())
                .build();

        ProductAttributeValue savedValue = attributeValueRepository.save(attributeValue);
        return ProductAttributeValueMapper.toResponse(savedValue);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueResponse getAttributeValueById(Long id) {
        ProductAttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + id));
        return ProductAttributeValueMapper.toResponse(attributeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueResponse> getValuesByAttributeId(Long attributeId) {
        List<ProductAttributeValue> values = attributeValueRepository.findByAttributeId(attributeId);
        return values.stream()
                .map(ProductAttributeValueMapper::toResponse)
                .toList();
    }

    @Override
    public ProductAttributeValueResponse updateAttributeValue(Long id, ProductAttributeValueRequest request) {
        ProductAttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + id));

        Long attributeId = attributeValue.getAttributeId();

        if (!attributeValue.getValue().equalsIgnoreCase(request.getValue()) && attributeValueRepository.existsByAttributeIdAndValue(attributeId, request.getValue())) {
            throw new DuplicateResourceException("Attribute value already exists for this attribute");
        }

        ProductAttributeValueMapper.updateEntity(attributeValue, request.getValue());
        ProductAttributeValue updatedValue = attributeValueRepository.save(attributeValue);
        return ProductAttributeValueMapper.toResponse(updatedValue);
    }

    @Override
    public void deleteAttributeValue(Long id) {
        ProductAttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + id));
        attributeValueRepository.delete(attributeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueResponse getAttributeValueByAttributeAndValue(Long attributeId, String value) {
        ProductAttributeValue attributeValue = attributeValueRepository.findByAttributeIdAndValueIgnoreCase(attributeId, value)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found"));
        return ProductAttributeValueMapper.toResponse(attributeValue);
    }
}
