package com.example.eshop.common.request;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    private static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".id";
    @Override protected boolean shouldNotFilterAsyncDispatch() { return false; }
    @Override protected boolean shouldNotFilterErrorDispatch() { return false; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String id = (String) request.getAttribute(ATTRIBUTE);
        if (id == null) {
            var headers = Collections.list(request.getHeaders(RequestIds.HEADER));
            id = RequestIds.resolve(headers.size() == 1 ? headers.get(0) : null);
            request.setAttribute(ATTRIBUTE, id);
        }
        MDC.put(RequestIds.MDC_KEY, id);
        response.setHeader(RequestIds.HEADER, id);
        var previous = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        var attributes = new org.springframework.web.context.request.ServletRequestAttributes(request, response);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attributes);
        try { chain.doFilter(request, response); }
        finally {
            MDC.remove(RequestIds.MDC_KEY);
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previous);
            attributes.requestCompleted();
        }
    }
}
