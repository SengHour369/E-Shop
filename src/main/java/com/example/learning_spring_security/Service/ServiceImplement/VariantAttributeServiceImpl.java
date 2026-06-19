//package com.example.learning_spring_security.Service.ServiceImplement;
//
//import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
//import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
//import com.example.learning_spring_security.Model.ProductAttribute;
//import com.example.learning_spring_security.Model.ProductAttributeValue;
//import com.example.learning_spring_security.Model.ProductSku;
//import com.example.learning_spring_security.Model.VariantAttribute;
//import com.example.learning_spring_security.Repository.ProductAttributeRepository;
//import com.example.learning_spring_security.Repository.ProductAttributeValueRepository;
//import com.example.learning_spring_security.Repository.ProductSkuRepository;
//import com.example.learning_spring_security.Repository.VariantAttributeRepository;
//import com.example.learning_spring_security.Service.ServiceStructure.VariantAttributeService;
//import com.example.learning_spring_security.ServiceMapper.VariantAttributeMapper;
//import com.example.learning_spring_security.dto.Request.VariantAttributeRequest;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class VariantAttributeServiceImpl implements VariantAttributeService {
//
//    private final VariantAttributeRepository variantAttributeRepository;
//    private final ProductSkuRepository productSkuRepository;
//    private final ProductAttributeRepository productAttributeRepository;
//    private final ProductAttributeValueRepository attributeValueRepository;
//
//    @Override
//    public ResponseErrorTemplate assignAttributeToVariant(VariantAttributeRequest request) {
//        ProductSku productSku = productSkuRepository.findById(request.getSkuId())
//                .orElseThrow(() -> new ResourceNotFoundException("ProductSku not found with id: " + request.getSkuId()));
//
//        ProductAttribute attribute = productAttributeRepository.findById(request.getAttributeId())
//                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + request.getAttributeId()));
//
//        ProductAttributeValue attributeValue = attributeValueRepository.findById(request.getAttributeValueId())
//                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + request.getAttributeValueId()));
//
//        // Verify the attribute value belongs to the attribute
//        if (!attributeValue.getAttribute().getId().equals(request.getAttributeId())) {
//            throw new IllegalArgumentException("Attribute value does not belong to the specified attribute");
//        }
//
//        // Check if this attribute is already assigned to this SKU with a different value
//        if (variantAttributeRepository.existsByProductSkuIdAndAttributeId(request.getSkuId(), request.getAttributeId())) {
//            throw new DuplicateResourceException("Attribute is already assigned to this SKU. Update it instead of creating a new one.");
//        }
//
//        VariantAttribute variantAttribute = VariantAttribute.builder()
//                .productSku(productSku)
//                .attribute(attribute)
//                .attributeValue(attributeValue)
//                .build();
//
//        VariantAttribute savedVariantAttribute = variantAttributeRepository.save(variantAttribute);
//        return VariantAttributeMapper.toResponse(savedVariantAttribute);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ResponseErrorTemplate getVariantAttribute(Long id) {
//        VariantAttribute variantAttribute = variantAttributeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));
//        return VariantAttributeMapper.toResponse(variantAttribute);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ResponseErrorTemplate> getAttributesByVariantId(Long skuId) {
//        // Verify SKU exists
//        productSkuRepository.findById(skuId)
//                .orElseThrow(() -> new ResourceNotFoundException("ProductSku not found with id: " + skuId));
//
//        List<VariantAttribute> variantAttributes = variantAttributeRepository.findByProductSkuId(skuId);
//        return variantAttributes.stream()
//                .map(VariantAttributeMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    public ResponseErrorTemplate updateVariantAttribute(Long id, VariantAttributeRequest request) {
//        VariantAttribute variantAttribute = variantAttributeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));
//
//        ProductAttributeValue newAttributeValue = attributeValueRepository.findById(request.getAttributeValueId())
//                .orElseThrow(() -> new ResourceNotFoundException("Attribute value not found with id: " + request.getAttributeValueId()));
//
//        // Verify the new attribute value belongs to the same attribute
//        if (!newAttributeValue.getAttribute().getId().equals(variantAttribute.getAttribute().getId())) {
//            throw new IllegalArgumentException("Attribute value does not belong to the existing attribute");
//        }
//
//        variantAttribute.setAttributeValue(newAttributeValue);
//        VariantAttribute updatedVariantAttribute = variantAttributeRepository.save(variantAttribute);
//        return VariantAttributeMapper.toResponse(updatedVariantAttribute);
//    }
//
//    @Override
//    public void removeAttributeFromVariant(Long id) {
//        VariantAttribute variantAttribute = variantAttributeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));
//        variantAttributeRepository.delete(variantAttribute);
//    }
//
//    @Override
//    public void removeAllAttributesFromVariant(Long skuId) {
//        // Verify SKU exists
//        productSkuRepository.findById(skuId)
//                .orElseThrow(() -> new ResourceNotFoundException("ProductSku not found with id: " + skuId));
//
//        List<VariantAttribute> variantAttributes = variantAttributeRepository.findByProductSkuId(skuId);
//        variantAttributeRepository.deleteAll(variantAttributes);
//    }
//}
//
//
//
