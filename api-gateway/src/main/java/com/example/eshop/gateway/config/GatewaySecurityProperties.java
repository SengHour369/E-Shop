package com.example.eshop.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
  private boolean authenticationEnabled;
  private String jwtSecret;
  private String adminKey;
  private List<String> publicPaths = new ArrayList<>();
  private List<String> cookieAllowedOrigins = List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080");

  public List<String> getCookieAllowedOrigins() { return cookieAllowedOrigins; }
  public void setCookieAllowedOrigins(List<String> value) { cookieAllowedOrigins = List.copyOf(value); }

  public boolean isAuthenticationEnabled() {
    return authenticationEnabled;
  }

  public void setAuthenticationEnabled(boolean value) {
    this.authenticationEnabled = value;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String value) {
    this.jwtSecret = value;
  }

  public String getAdminKey() {
    return adminKey;
  }

  public void setAdminKey(String value) {
    this.adminKey = value;
  }

  public List<String> getPublicPaths() {
    return publicPaths;
  }

  public void setPublicPaths(List<String> value) {
    this.publicPaths = value;
  }
}
