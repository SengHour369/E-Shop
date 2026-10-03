package com.example.eshop.gateway.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

@Table("gateway_admin_keys")
public class GatewayAdminKey {
    @Id private Short id;
    private String adminKey;
    private Instant createdAt;
    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    @JsonIgnore public String getAdminKey() { return adminKey; }
    public void setAdminKey(String adminKey) { this.adminKey = adminKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
