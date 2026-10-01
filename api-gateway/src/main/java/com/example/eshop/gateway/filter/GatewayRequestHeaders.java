package com.example.eshop.gateway.filter;

/** Headers and exchange attributes shared by the gateway request-flow filters. */
public final class GatewayRequestHeaders {

  public static final String CORRELATION_ID = "X-Correlation-Id";
  public static final String CORRELATION_ID_ATTRIBUTE = "gatewayCorrelationId";
  public static final String AUTHENTICATED_USERNAME_ATTRIBUTE = "gatewayAuthenticatedUsername";
  public static final String AUTHENTICATED_USERNAME = "X-Authenticated-Username";

  private GatewayRequestHeaders() {}
}
