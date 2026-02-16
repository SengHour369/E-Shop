package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.AddressService;
import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "address-controller", description = "Address management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class AddressController extends BaseController {

    private final AddressService addressService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user addresses", description = "Get all addresses for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            @Parameter(description = "User ID", example = "1", required = true)
            @PathVariable Long userId) {
        List<AddressResponse> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/user/{userId}/paged")
    @Operation(summary = "Get user addresses with pagination", description = "Get paginated addresses for a user")
    public ResponseEntity<Page<AddressResponse>> getUserAddressesPaged(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AddressResponse> addresses = addressService.getUserAddresses(userId, pageable);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Get address details by ID")
    public ResponseEntity<AddressResponse> getAddressById(
            @Parameter(description = "Address ID", example = "1") @PathVariable Long id) {
        AddressResponse address = addressService.getAddressById(id);
        return ResponseEntity.ok(address);
    }

    @GetMapping("/user/{userId}/default")
    @Operation(summary = "Get default address", description = "Get default address for a user")
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        AddressResponse address = addressService.getDefaultAddress(userId);
        return ResponseEntity.ok(address);
    }

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create address", description = "Create a new address for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<AddressResponse> createAddress(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/user/{userId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "Address ID", example = "1") @PathVariable Long id,
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{addressId}/user/{userId}/set-default")
    @Operation(summary = "Set default address", description = "Set an address as default for a user")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @Parameter(description = "Address ID", example = "1") @PathVariable Long addressId,
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        AddressResponse response = addressService.setDefaultAddress(addressId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/user/{userId}")
    @Operation(summary = "Delete address", description = "Delete an address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "Address ID", example = "1") @PathVariable Long id,
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        addressService.deleteAddress(id, userId);
        return ResponseEntity.noContent().build();
    }
}