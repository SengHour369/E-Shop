//package com.example.learning_spring_security.ServiceMapper;
//
//import com.example.learning_spring_security.Constant.Constant;
//import com.example.learning_spring_security.Model.VariantAttribute;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import com.example.learning_spring_security.dto.Response.VariantAttributeResponse;
//
//public class VariantAttributeMapper {
//
//    public static ResponseErrorTemplate toResponse(VariantAttribute variantAttribute) {
//        VariantAttributeResponse response = VariantAttributeResponse.builder()
//                .id(variantAttribute.getId())
//                .productSkuId(variantAttribute.getProductSku().getId())
//                .attributeId(variantAttribute.getAttribute().getId())
//                .attributeName(variantAttribute.getAttribute().getName())
//                .attributeValueId(variantAttribute.getAttributeValue().getId())
//                .attributeValue(variantAttribute.getAttributeValue().getValue())
//                .build();
//        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
//    }
//
//    public static VariantAttributeResponse toResponseDTO(VariantAttribute variantAttribute) {
//        return VariantAttributeResponse.builder()
//                .id(variantAttribute.getId())
//                .productSkuId(variantAttribute.getProductSku().getId())
//                .attributeId(variantAttribute.getAttribute().getId())
//                .attributeName(variantAttribute.getAttribute().getName())
//                .attributeValueId(variantAttribute.getAttributeValue().getId())
//                .attributeValue(variantAttribute.getAttributeValue().getValue())
//                .build();
//    }
//}
//
