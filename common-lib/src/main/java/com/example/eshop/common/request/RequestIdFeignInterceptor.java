package com.example.eshop.common.request;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestIdFeignInterceptor implements RequestInterceptor {
    @Override public void apply(RequestTemplate template) {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            String parent = servlet.getRequest().getHeader("traceparent");
            if (TraceContextFilter.valid(parent) && !template.headers().containsKey("traceparent")) template.header("traceparent", parent);
        }
        String id = RequestIds.current();
        if (id != null) {
            template.removeHeader(RequestIds.HEADER);
            template.header(RequestIds.HEADER, id);
        }
    }
}
