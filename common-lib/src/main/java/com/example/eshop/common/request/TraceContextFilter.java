package com.example.eshop.common.request;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.slf4j.MDC;
import java.io.IOException;
/** Passive W3C propagation when no tracing bridge is installed; does not fabricate spans. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceContextFilter extends OncePerRequestFilter {
    public static boolean valid(String value) {
        return value != null && value.matches("00-(?!0{32})[a-f0-9]{32}-(?!0{16})[a-f0-9]{16}-[a-f0-9]{2}");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String parent=request.getHeader("traceparent");
        String previous=MDC.get("traceId");
        if(previous==null && valid(parent)) MDC.put("traceId",parent.substring(3,35));
        try { chain.doFilter(request,response); }
        finally { if(previous==null) MDC.remove("traceId"); else MDC.put("traceId",previous); }
    }
}
