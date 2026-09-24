package com.example.learning_spring_security.ConfigSecurity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place on any controller method to enforce that the authenticated user
 * has an active UserPermission row for the given funcId.
 *
 * Example:
 * @RequirePermission(funcId = 10L)
 * @PostMapping("/admin/reports")
 * public ResponseEntity<?> getReports() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    long funcId();
}