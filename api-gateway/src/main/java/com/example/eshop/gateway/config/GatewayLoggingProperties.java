package com.example.eshop.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.logging")
public class GatewayLoggingProperties {
  private boolean databaseEnabled = true;

  public boolean isDatabaseEnabled() {
    return databaseEnabled;
  }

  public void setDatabaseEnabled(boolean value) {
    this.databaseEnabled = value;
  }
}
