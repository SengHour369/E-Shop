package com.example.eshop.common.request;
import feign.RequestTemplate;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.slf4j.MDC;
import static org.assertj.core.api.Assertions.*;
class RequestIdTest {
    @AfterEach void clean() { MDC.clear(); RequestContextHolder.resetRequestAttributes(); }
    @Test void preservesValidAndCleansEvenOnFailure() {
        var request = new MockHttpServletRequest(); request.addHeader(RequestIds.HEADER, "req_support-42");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new RequestIdFilter().doFilter(request, response, (req, res) -> {
            assertThat(RequestIds.current()).isEqualTo("req_support-42");
            assertThat(RequestContextHolder.getRequestAttributes()).isNotNull();
            throw new jakarta.servlet.ServletException("failure");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(response.getHeader(RequestIds.HEADER)).isEqualTo("req_support-42");
        assertThat(RequestIds.current()).isNull();
        assertThat(RequestContextHolder.getRequestAttributes()).isNull();
    }
    @Test void generatesForMissingMalformedLongAndDuplicateHeaders() throws Exception {
        for (String input : new String[] {"", "bad value", "x".repeat(129), "bad\r\nheader"}) {
            var request = new MockHttpServletRequest(); request.addHeader(RequestIds.HEADER, input);
            var response = new MockHttpServletResponse();
            new RequestIdFilter().doFilter(request, response, (req, res) ->
                assertThat(RequestIds.current()).startsWith("req_").hasSize(40));
            assertThat(response.getHeader(RequestIds.HEADER)).isNotEqualTo(input);
        }
        var missing = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(new MockHttpServletRequest(), missing, (req, res) -> {});
        assertThat(missing.getHeader(RequestIds.HEADER)).startsWith("req_");
        var duplicate = new MockHttpServletRequest();
        duplicate.addHeader(RequestIds.HEADER, "first"); duplicate.addHeader(RequestIds.HEADER, "second");
        var response = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(duplicate, response, (req, res) -> {});
        assertThat(response.getHeader(RequestIds.HEADER)).startsWith("req_");
    }
    @Test void sameIdSurvivesThreeServiceHops() throws Exception {
        String id = "req_chain";
        for (int hop = 0; hop < 3; hop++) {
            var incoming = new MockHttpServletRequest(); incoming.addHeader(RequestIds.HEADER, id);
            var outgoing = new RequestTemplate(); outgoing.header(RequestIds.HEADER, "stale");
            var response = new MockHttpServletResponse();
            new RequestIdFilter().doFilter(incoming, response, (req, res) -> new RequestIdFeignInterceptor().apply(outgoing));
            assertThat(outgoing.headers().get(RequestIds.HEADER)).containsExactly(id);
            assertThat(response.getHeader(RequestIds.HEADER)).isEqualTo(id);
            assertThat(RequestIds.current()).isNull();
        }
    }
    @Test void asyncDecoratorRestoresWorkerContextOnFailure() {
        MDC.put("requestId", "req_parent");
        Runnable decorated = new RequestContextConfiguration().requestContextTaskDecorator().decorate(() -> {
            assertThat(RequestIds.current()).isEqualTo("req_parent");
            throw new IllegalStateException();
        });
        MDC.put("requestId", "worker");
        assertThatThrownBy(decorated::run).isInstanceOf(IllegalStateException.class);
        assertThat(RequestIds.current()).isEqualTo("worker");
    }

    @Test void restClientPropagatesCurrentId() {
        MDC.put("requestId", "req_rest");
        var builder = org.springframework.web.client.RestClient.builder();
        new RequestContextConfiguration().requestIdRestClientCustomizer().customize(builder);
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://downstream/test"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("X-Request-ID", "req_rest"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess());
        builder.build().get().uri("http://downstream/test").retrieve().toBodilessEntity();
        server.verify();
    }
}
