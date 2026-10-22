package com.example.eshop.auth.config;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class GroupPermissionInterceptorTest {
 @Mock UserRepository users;
 @Mock FunctionPermissionRepository functions;
 @Mock ApiPermissionRepository apis;
 @InjectMocks ApiPermissionInterceptor interceptor;
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void groupAdminAuthorityDoesNotBypassEffectiveGrants() {
  var user=new User();user.setId(43L);user.setUsername("member");user.setRoles(List.of(Role.builder().name("USER").build()));
  when(users.findByUsername("member")).thenReturn(Optional.of(user));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("member",null,List.of(new SimpleGrantedAuthority("ADMIN"))));
  var api=mock(ApiPermission.class);when(api.getApi()).thenReturn("/test");when(api.getFuncId()).thenReturn(10L);
  when(apis.findActiveByMethod("GET")).thenReturn(List.of(api));
  when(functions.findById(10L)).thenReturn(Optional.of(FunctionPermission.builder().funcId(10L).funcCode("PRODUCT_VIEW").build()));
  when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_VIEW"),Set.of());
  var request=new MockHttpServletRequest("GET","/test");
  assertThat(interceptor.preHandle(request,new MockHttpServletResponse(),new Object())).isTrue();
  assertThatThrownBy(()->interceptor.preHandle(request,new MockHttpServletResponse(),new Object())).hasMessageContaining("permission");
 }
}
