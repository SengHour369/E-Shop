package com.example.eshop.common.request;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestIdFeignInterceptor implements RequestInterceptor {
    @Override public void apply(RequestTemplate template) {
        String id = RequestIds.current();
        if (id != null) {
            template.removeHeader(RequestIds.HEADER);
            template.header(RequestIds.HEADER, id);
        }
    }
}
