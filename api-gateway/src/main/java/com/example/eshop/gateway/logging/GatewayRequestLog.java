package com.example.eshop.gateway.logging;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("gateway_request_logs")
public class GatewayRequestLog {
  @Id private Long id;
  private String correlationId;
  private String method;
  private String path;
  private Integer responseStatus;
  private long durationMs;
  private String clientIp;
  private int sort = 0;
  private boolean deleted = false;
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String value) {
    correlationId = value;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String value) {
    method = value;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String value) {
    path = value;
  }

  public Integer getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(Integer value) {
    responseStatus = value;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(long value) {
    durationMs = value;
  }

  public String getClientIp() {
    return clientIp;
  }

  public void setClientIp(String value) {
    clientIp = value;
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
}
