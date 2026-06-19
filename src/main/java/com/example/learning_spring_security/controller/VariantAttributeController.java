//package com.example.learning_spring_security.controller;
//
//import com.example.learning_spring_security.Service.ServiceStructure.VariantAttributeService;
//import com.example.learning_spring_security.dto.Request.VariantAttributeRequest;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/variant-attributes")
//@RequiredArgsConstructor
//@Tag(name = "variant-attribute-controller", description = "SKU variant attribute management APIs")
//public class VariantAttributeController extends BaseController {
//
//    private final VariantAttributeService variantAttributeService;
//
//    @PostMapping
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Assign attribute to SKU", description = "Assign a product attribute with a specific value to a SKU (Admin only)")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "201", description = "Attribute assigned successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid request"),
//            @ApiResponse(responseCode = "404", description = "SKU, attribute, or attribute value not found"),
//            @ApiResponse(responseCode = "409", description = "Attribute already assigned to this SKU")
//    })
//    public ResponseEntity<ResponseErrorTemplate> assignAttributeToVariant(
//            @Valid @RequestBody VariantAttributeRequest request) {
//        ResponseErrorTemplate response = variantAttributeService.assignAttributeToVariant(request);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }
//
//    @GetMapping("/{id}")
//    @Operation(summary = "Get variant attribute by ID", description = "Retrieve a specific variant attribute by ID")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Variant attribute found"),
//            @ApiResponse(responseCode = "404", description = "Variant attribute not found")
//    })
//    public ResponseEntity<ResponseErrorTemplate> getVariantAttribute(
//            @Parameter(description = "Variant attribute ID", example = "1")
//            @PathVariable Long id) {
//        ResponseErrorTemplate response = variantAttributeService.getVariantAttribute(id);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/sku/{skuId}")
//    @Operation(summary = "Get attributes by SKU ID", description = "Retrieve all attributes assigned to a specific SKU")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Attributes retrieved successfully"),
//            @ApiResponse(responseCode = "404", description = "SKU not found")
//    })
//    public ResponseEntity<List<ResponseErrorTemplate>> getAttributesByVariantId(
//            @Parameter(description = "SKU ID", example = "1")
//            @PathVariable Long skuId) {
//        List<ResponseErrorTemplate> response = variantAttributeService.getAttributesByVariantId(skuId);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Update variant attribute", description = "Update the attribute value for a specific SKU attribute assignment (Admin only)")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Variant attribute updated successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid request"),
//            @ApiResponse(responseCode = "404", description = "Variant attribute or attribute value not found"),
//            @ApiResponse(responseCode = "409", description = "Attribute value does not belong to the same attribute")
//    })
//    public ResponseEntity<ResponseErrorTemplate> updateVariantAttribute(
//            @Parameter(description = "Variant attribute ID", example = "1")
//            @PathVariable Long id,
//            @Valid @RequestBody VariantAttributeRequest request) {
//        ResponseErrorTemplate response = variantAttributeService.updateVariantAttribute(id, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Remove attribute from SKU", description = "Remove a specific attribute assignment from a SKU (Admin only)")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "204", description = "Attribute removed successfully"),
//            @ApiResponse(responseCode = "404", description = "Variant attribute not found")
//    })
//    public ResponseEntity<Void> removeAttributeFromVariant(
//            @Parameter(description = "Variant attribute ID", example = "1")
//            @PathVariable Long id) {
//        variantAttributeService.removeAttributeFromVariant(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("/sku/{skuId}/all")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Remove all attributes from SKU", description = "Remove all attribute assignments from a specific SKU (Admin only)")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "204", description = "All attributes removed successfully"),
//            @ApiResponse(responseCode = "404", description = "SKU not found")
//    })
//    public ResponseEntity<Void> removeAllAttributesFromVariant(
//            @Parameter(description = "SKU ID", example = "1")
//            @PathVariable Long skuId) {
//        variantAttributeService.removeAllAttributesFromVariant(skuId);
//        return ResponseEntity.noContent().build();
//    }
//}
//
