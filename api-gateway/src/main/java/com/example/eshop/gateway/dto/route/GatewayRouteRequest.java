package com.example.eshop.gateway.dto.route;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GatewayRouteRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9-]+") String routeKey,
    @NotBlank @Pattern(regexp = "lb://[a-z0-9-]+") String uri,
    @NotBlank @Pattern(regexp = "/api/.*") String pathPattern,
    @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") String httpMethod,
    boolean enabled,
    @Min(1) Integer rateLimit,
    @Min(1) Integer rateLimitWindowSeconds) {}
