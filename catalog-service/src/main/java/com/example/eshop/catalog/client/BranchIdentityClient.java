package com.example.eshop.catalog.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="auth-service",contextId="branchIdentity")
public interface BranchIdentityClient {

 @GetMapping("/internal/authorization/users/{id}/active")
 boolean active(@RequestHeader("Authorization")
                String token,@PathVariable("id") long id);
}
