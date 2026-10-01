package com.example.eshop.common.request;

import org.slf4j.MDC;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration
public class RequestContextConfiguration {
    @Bean public RestClientCustomizer requestIdRestClientCustomizer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            if (RequestIds.current() != null) request.getHeaders().set(RequestIds.HEADER, RequestIds.current());
            return execution.execute(request, body);
        });
    }
    @Bean public TaskDecorator requestContextTaskDecorator() {
        return task -> {
            var captured = MDC.getCopyOfContextMap();
            return () -> {
                var previous = MDC.getCopyOfContextMap();
                try {
                    if (captured == null) MDC.clear(); else MDC.setContextMap(captured);
                    task.run();
                } finally {
                    if (previous == null) MDC.clear(); else MDC.setContextMap(previous);
                }
            };
        };
    }
}
