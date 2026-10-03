package com.example.eshop.common.request;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;
import org.slf4j.MDC;
import feign.RequestTemplate;
import static org.assertj.core.api.Assertions.*;
class TracePropagationTest {
    @AfterEach void clear() { MDC.clear(); RequestContextHolder.resetRequestAttributes(); }
    @Test void validatedTraceAndRequestIdPassToFeignAndMdcIsRestored() throws Exception {
        String parent="00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";
        var request=new MockHttpServletRequest(); request.addHeader("traceparent",parent);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request)); MDC.put("requestId","req_chain");
        new TraceContextFilter().doFilter(request,new MockHttpServletResponse(),(r,s)->{
            var template=new RequestTemplate(); new RequestIdFeignInterceptor().apply(template);
            assertThat(template.headers().get("X-Request-ID")).containsExactly("req_chain");
            assertThat(template.headers().get("traceparent")).containsExactly(parent);
            assertThat(MDC.get("traceId")).isEqualTo("0123456789abcdef0123456789abcdef");
        });
        assertThat(MDC.get("traceId")).isNull();
        assertThat(TraceContextFilter.valid("00-"+"0".repeat(32)+"-0123456789abcdef-01")).isFalse();
    }
}
