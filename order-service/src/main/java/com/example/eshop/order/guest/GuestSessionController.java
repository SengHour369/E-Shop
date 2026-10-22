package com.example.eshop.order.guest;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/guest/session") @RequiredArgsConstructor
public class GuestSessionController {
    private final GuestSessionService service;
    @PostMapping public GuestSessionService.SessionView create(HttpServletRequest request,HttpServletResponse response) {
        return service.establish(request,response);
    }
    @DeleteMapping public void revoke(HttpServletRequest request,HttpServletResponse response) { service.revoke(request,response); }
}
