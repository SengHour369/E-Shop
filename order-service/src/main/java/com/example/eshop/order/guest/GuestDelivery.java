package com.example.eshop.order.guest;
import jakarta.validation.constraints.*;

/** Copied into the order once; never stored in Identity's customer-address namespace. */
public record GuestDelivery(
        @NotBlank @Size(max=120) String fullName,
        @NotBlank @Pattern(regexp="(?=(?:[^0-9]*[0-9]){7,15}[^0-9]*$)[+0-9 ()-]{7,30}") String phone,
        @Email @Size(max=254) String email,
        @NotBlank @Size(max=100) String province,
        @NotBlank @Size(max=100) String district,
        @NotBlank @Size(max=100) String commune,
        @NotBlank @Size(max=100) String village,
        @NotBlank @Size(max=250) String streetHouse,
        @Size(max=500) String notes) { }
