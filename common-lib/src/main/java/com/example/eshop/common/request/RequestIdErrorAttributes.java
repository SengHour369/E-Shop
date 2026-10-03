package com.example.eshop.common.request;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.web.context.request.WebRequest;
import java.util.Map;
@Component
public class RequestIdErrorAttributes extends DefaultErrorAttributes {
    @Override public Map<String,Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
        var attributes = super.getErrorAttributes(request, options);
        attributes.put("requestId", RequestIds.current());
        return attributes;
    }
}
