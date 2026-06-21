package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.ProductAttributeValueService;
import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attribute-values")
@RequiredArgsConstructor
@Tag(name = "product-attribute-value-controller", description = "Product attribute value management APIs")
public class ProductAttributeValueController extends BaseController {

    private final ProductAttributeValueService productAttributeValueService;

    @GetMapping("/{id}")
    @Operation(summary = "Get attribute value by ID", description = "Retrieve a specific attribute value by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute value found"),
            @ApiResponse(responseCode = "404", description = "Attribute value not found")
    })
    public ResponseEntity<ProductAttributeValueResponse> getAttributeValueById(
            @Parameter(description = "Attribute value ID", example = "1")
            @PathVariable Long id) {
        ProductAttributeValueResponse response = productAttributeValueService.getAttributeValueById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attribute/{attributeId}")
    @Operation(summary = "Get values by attribute ID", description = "Retrieve all values for a specific attribute")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Values retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Attribute not found")
    })
    public ResponseEntity<List<ProductAttributeValueResponse>> getValuesByAttributeId(
            @Parameter(description = "Attribute ID", example = "1")
            @PathVariable Long attributeId) {
        List<ProductAttributeValueResponse> response = productAttributeValueService.getValuesByAttributeId(attributeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update attribute value", description = "Update an existing attribute value (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute value updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Attribute value not found"),
            @ApiResponse(responseCode = "409", description = "Attribute value already exists")
    })
    public ResponseEntity<ProductAttributeValueResponse> updateAttributeValue(
            @Parameter(description = "Attribute value ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductAttributeValueRequest request) {
        ProductAttributeValueResponse response = productAttributeValueService.updateAttributeValue(
                id, 
                request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete attribute value", description = "Delete an attribute value by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attribute value deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Attribute value not found")
    })
    public ResponseEntity<Void> deleteAttributeValue(
            @Parameter(description = "Attribute value ID", example = "1")
            @PathVariable Long id) {
        productAttributeValueService.deleteAttributeValue(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get attribute value by attribute and value", description = "Retrieve a specific attribute value by attribute ID and value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute value found"),
            @ApiResponse(responseCode = "404", description = "Attribute value not found")
    })
    public ResponseEntity<ProductAttributeValueResponse> getAttributeValueByAttributeAndValue(
            @Parameter(description = "Attribute ID", example = "1")
            @RequestParam Long attributeId,
            @Parameter(description = "Attribute value", example = "Red")
            @RequestParam String value) {
        ProductAttributeValueResponse response = productAttributeValueService.getAttributeValueByAttributeAndValue(attributeId, value);
        return ResponseEntity.ok(response);
    }
}

