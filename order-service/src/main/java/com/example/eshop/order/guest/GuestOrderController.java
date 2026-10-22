package com.example.eshop.order.guest;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController @RequestMapping("/api/v1/guest") @RequiredArgsConstructor
public class GuestOrderController {
    private final GuestSessionService sessions;
    private final GuestOrderService orders;
    @PostMapping("/checkout/preview") public Object preview(HttpServletRequest request,@Valid @RequestBody GuestCheckoutRequest body) {
        return orders.preview(sessions.require(request,true),body);
    }
    @PostMapping("/orders") public Object create(HttpServletRequest request,@Valid @RequestBody GuestCheckoutRequest body) {
        return orders.create(sessions.require(request,true),body);
    }
    @GetMapping("/orders") public ResponseEntity<?> list(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.list(sessions.require(request,false)));
    }
    @PostMapping("/orders/{id}/access") public Object access(HttpServletRequest request,@PathVariable long id) {
        return orders.issue(sessions.require(request,true),id);
    }
    @GetMapping("/orders/{id}") public ResponseEntity<?> detail(HttpServletRequest request,@PathVariable long id,
            @RequestHeader(value="X-Guest-Order-Token",required=false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.detail(sessions.require(request,false),id,token));
    }
    @PostMapping("/orders/{id}/payments") public Object initiate(HttpServletRequest request,@PathVariable long id,
            @RequestHeader(value="X-Guest-Order-Token",required=false) String token) {
        return orders.payment(sessions.require(request,true),id,token,true);
    }
    @GetMapping(value="/orders/{id}/payment-image",produces="image/png") public ResponseEntity<byte[]> image(HttpServletRequest request,@PathVariable long id,
            @RequestHeader(value="X-Guest-Order-Token",required=false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.paymentImage(sessions.require(request,false),id,token));
    }
    @GetMapping("/orders/{id}/payment-status") public ResponseEntity<?> payment(HttpServletRequest request,@PathVariable long id,
            @RequestHeader(value="X-Guest-Order-Token",required=false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.payment(sessions.require(request,false),id,token,false));
    }
}
