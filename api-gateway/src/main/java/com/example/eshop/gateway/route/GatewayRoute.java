package com.example.eshop.gateway.route;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("gateway_routes")
public class GatewayRoute {
  @Id private Long id;
  private String routeKey;
  private String uri;
  private String pathPattern;
  private String httpMethod;
  private boolean enabled = true;
  private Integer rateLimit;
  private Integer rateLimitWindowSeconds;
  private int sort = 0;
  private boolean deleted = false;
  private Instant createdAt;
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getRouteKey() {
    return routeKey;
  }

  public void setRouteKey(String value) {
    routeKey = value;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String value) {
    uri = value;
  }

  public String getPathPattern() {
    return pathPattern;
  }

  public void setPathPattern(String value) {
    pathPattern = value;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String value) {
    httpMethod = value;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean value) {
    enabled = value;
  }

  public Integer getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(Integer value) {
    rateLimit = value;
  }

  public Integer getRateLimitWindowSeconds() {
    return rateLimitWindowSeconds;
  }

  public void setRateLimitWindowSeconds(Integer value) {
    rateLimitWindowSeconds = value;
  }

  public int getSort() {
    return sort;
  }

  public void setSort(int value) {
    sort = value;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean value) {
    deleted = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }
}
