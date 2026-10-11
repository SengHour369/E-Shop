package com.example.eshop.auth.controller;
import com.example.eshop.auth.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequiredArgsConstructor
public class CommerceAddressController {
    private final AddressRepository addresses;
    @GetMapping("/internal/commerce/addresses/{id}/owner/{userId}")
    @PreAuthorize("hasAuthority('SERVICE_ORDER')")
    public boolean owned(@PathVariable long id,@PathVariable long userId) {return addresses.isUserHasAddress(userId,id);}
}
