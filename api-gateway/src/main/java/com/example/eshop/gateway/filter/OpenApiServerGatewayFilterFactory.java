package com.example.eshop.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Keeps Swagger requests on the public gateway instead of internal Docker addresses. */
@Component
public class OpenApiServerGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
  private final ModifyResponseBodyGatewayFilterFactory responseBody;
  private final ObjectMapper objectMapper;

  public OpenApiServerGatewayFilterFactory(
      ModifyResponseBodyGatewayFilterFactory responseBody, ObjectMapper objectMapper) {
    super(Object.class);
    this.responseBody = responseBody;
    this.objectMapper = objectMapper;
  }

  @Override
  public GatewayFilter apply(Object config) {
    return responseBody.apply(
        new ModifyResponseBodyGatewayFilterFactory.Config()
            .setRewriteFunction(String.class, String.class, (exchange, body) -> {
              if (body == null) {
                return Mono.empty();
              }
              try {
                var document = objectMapper.readTree(body);
                if (document instanceof ObjectNode root && root.has("openapi")) {
                  root.putArray("servers").addObject().put("url", "/");
                  return Mono.just(objectMapper.writeValueAsString(root));
                }
                return Mono.just(body);
              } catch (Exception exception) {
                return Mono.error(exception);
              }
            }));
  }
}
