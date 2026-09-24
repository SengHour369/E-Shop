package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.ProductAttributeService;
import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
@Tag(name = "product-attribute-controller", description = "Product attribute management APIs")
public class ProductAttributeController extends BaseController {

    private final ProductAttributeService productAttributeService;

    @GetMapping("/{id}")
    @Operation(summary = "Get attribute by ID", description = "Retrieve a specific attribute by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute found"),
            @ApiResponse(responseCode = "404", description = "Attribute not found")
    })
    public ResponseEntity<ProductAttributeResponse> getAttributeById(
            @Parameter(description = "Attribute ID", example = "1")
            @PathVariable Long id) {
        ProductAttributeResponse response = productAttributeService.getAttributeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get attribute by name", description = "Retrieve a specific attribute by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attribute found"),
            @ApiResponse(responseCode = "404", description = "Attribute not found")
    })
    public ResponseEntity<ProductAttributeResponse> getAttributeByName(
            @Parameter(description = "Attribute name", example = "Color")
            @PathVariable String name) {
        ProductAttributeResponse response = productAttributeService.getAttributeByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all attributes", description = "Retrieve all product attributes")
    public ResponseEntity<List<ProductAttributeResponse>> getAllAttributes() {
        List<ProductAttributeResponse> response = productAttributeService.getAllAttributes();
        return ResponseEntity.ok(response);
    }

//    @PutMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Update attribute", description = "Update an existing attribute (Admin only)")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Attribute updated successfully"),
//            @ApiResponse(responseCode = "404", description = "Attribute not found"),
//            @ApiResponse(responseCode = "409", description = "Attribute name already exists")
//    })
//    public ResponseEntity<ProductAttributeResponse> updateAttribute(
//            @Parameter(description = "Attribute ID", example = "1")
//            @PathVariable Long id,
//            @Valid @RequestBody ProductAttributeRequest request) {
//        ProductAttributeResponse response = productAttributeService.updateAttribute(id, request);
//        return ResponseEntity.ok(response);
//    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete attribute", description = "Delete an attribute by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attribute deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Attribute not found")
    })
    public ResponseEntity<Void> deleteAttribute(
            @Parameter(description = "Attribute ID", example = "1")
            @PathVariable Long id) {
        productAttributeService.deleteAttribute(id);
        return ResponseEntity.noContent().build();
    }
}
