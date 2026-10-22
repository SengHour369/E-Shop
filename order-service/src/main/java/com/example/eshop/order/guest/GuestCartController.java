package com.example.eshop.order.guest;

import com.example.eshop.order.service.impl.CartServiceImpl;
import com.example.eshop.order.dto.request.CartRequest;
import com.example.eshop.common.storefront.StorefrontAccessClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/guest/cart") @RequiredArgsConstructor @Transactional
public class GuestCartController {
    private final GuestSessionService sessions;
    private final CartServiceImpl carts;
    private final StorefrontAccessClient access;
    private GuestSession require(HttpServletRequest request,boolean mutation) {
        access.require(access.current().guestCart());return sessions.require(request,mutation);
    }
    @GetMapping public Object get(HttpServletRequest request) { return carts.guestCart(require(request,false).getId()); }
    @PostMapping("/items") public Object add(HttpServletRequest request,@Valid @RequestBody CartRequest body) {
        return carts.guestAdd(require(request,true).getId(),body);
    }
    @PatchMapping("/items/{id}") public Object update(HttpServletRequest request,@PathVariable long id,@Valid @RequestBody CartRequest body) {
        return carts.guestUpdate(require(request,true).getId(),id,body);
    }
    @DeleteMapping("/items") public Object clear(HttpServletRequest request) {return carts.guestClear(require(request,true).getId());}
    @DeleteMapping("/items/{id}") public Object remove(HttpServletRequest request,@PathVariable long id) {
        return carts.guestRemove(require(request,true).getId(),id);
    }
}
