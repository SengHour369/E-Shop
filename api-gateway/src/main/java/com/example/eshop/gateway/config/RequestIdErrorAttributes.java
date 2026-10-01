package com.example.eshop.gateway.config;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.web.reactive.function.server.ServerRequest;
import java.util.Map;
@Component
public class RequestIdErrorAttributes extends DefaultErrorAttributes {
    @Override public Map<String,Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        var attributes = super.getErrorAttributes(request, options);
        attributes.put("requestId", request.attribute("gatewayCorrelationId").orElse("unknown"));
        return attributes;
    }
}
