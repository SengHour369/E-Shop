package com.example.eshop.common.security;
import com.example.eshop.common.jwt.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GroupPermissionFilterTest {
 @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 @Test void deniedGroupCannotPassAdminRoleCheck() throws Exception { check(false); }
 @Test void grantedGroupProceeds() throws Exception { check(true); }
 void check(boolean allowed) throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/internal/authorization/api-access",e->{byte[] b=String.valueOf(allowed).getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});server.start();
  try {
   var p=new JwtProperties();ReflectionTestUtils.setField(p,"header","Authorization");ReflectionTestUtils.setField(p,"prefix","Bearer");ReflectionTestUtils.setField(p,"authorizationUrl","http://127.0.0.1:"+server.getAddress().getPort());
   var validator=mock(JwtTokenValidator.class);var claims=Jwts.claims();claims.setSubject("member");claims.put("isEnable",true);claims.put("authorities",List.of("ADMIN","GROUP_PERMISSION_SCOPED"));when(validator.parseClaims("test")).thenReturn(claims);
   var request=new MockHttpServletRequest("GET","/api/v1/orders/summary");request.addHeader("Authorization","Bearer test");var response=new MockHttpServletResponse();var chain=mock(jakarta.servlet.FilterChain.class);
   new JwtAuthFilter(validator,p).doFilter(request,response,chain);
   if(allowed)verify(chain).doFilter(request,response);else {assertThat(response.getStatus()).isEqualTo(403);verifyNoInteractions(chain);}
  }finally{server.stop(0);}
 }
}
