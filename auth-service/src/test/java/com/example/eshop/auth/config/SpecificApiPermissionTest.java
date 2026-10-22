package com.example.eshop.auth.config;
import com.example.eshop.auth.controller.InternalApiAccessController;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.service.AccountAccessService;
import com.example.eshop.common.security.PermissionSnapshot;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class SpecificApiPermissionTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void broadCreateGrantCannotAuthorizeDeleteThroughEitherAuthorizationPath(){
  var users=mock(UserRepository.class);var functions=mock(FunctionPermissionRepository.class);var apis=mock(ApiPermissionRepository.class);
  var user=User.builder().id(43L).username("member").roles(List.of(Role.builder().name("USER").build())).build();
  when(users.findByUsername("member")).thenReturn(Optional.of(user));when(users.findById(43L)).thenReturn(Optional.of(user));
  when(apis.findActiveByMethod("POST")).thenReturn(List.of(
   ApiPermission.builder().method("POST").api("/api/v1/products/**").funcId(1003L).build(),
   ApiPermission.builder().method("POST").api("/api/v1/products/delete/").funcId(1006L).build()));
  when(functions.findById(1006L)).thenReturn(Optional.of(FunctionPermission.builder().funcId(1006L).funcCode("PRODUCT_DELETE").build()));
  var auth=new UsernamePasswordAuthenticationToken("member",null,List.of());auth.setDetails(Map.of("userId",43L));SecurityContextHolder.getContext().setAuthentication(auth);
  var interceptor=new ApiPermissionInterceptor(apis,functions,null,users,null,null);
  var access=mock(AccountAccessService.class);var internal=new InternalApiAccessController(users,access,apis,functions);
  var request=new MockHttpServletRequest("POST","/api/v1/products/delete/");
  when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_CREATE"));
  when(access.snapshot(user)).thenReturn(new PermissionSnapshot(43L,true,Set.of("PRODUCT_CREATE")));
  assertThatThrownBy(()->interceptor.preHandle(request,new MockHttpServletResponse(),new Object())).hasMessageContaining("permission");
  assertThat(internal.allowed(auth,"POST",request.getRequestURI())).isFalse();
  when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_DELETE"));
  when(access.snapshot(user)).thenReturn(new PermissionSnapshot(43L,true,Set.of("PRODUCT_DELETE")));
  assertThat(interceptor.preHandle(request,new MockHttpServletResponse(),new Object())).isTrue();
  assertThat(internal.allowed(auth,"POST",request.getRequestURI())).isTrue();
 }
}
