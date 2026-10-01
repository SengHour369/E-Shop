package com.example.eshop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Single public entry point. Routes are declared in {@code application.yml} and resolved against
 * the Eureka registry ({@code lb://<service-id>}), so a service can scale to multiple instances
 * without the gateway config changing.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
