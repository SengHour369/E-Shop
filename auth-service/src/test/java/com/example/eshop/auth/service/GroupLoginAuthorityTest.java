package com.example.eshop.auth.service;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GroupLoginAuthorityTest {
 @Test void groupAdministratorGetsLoginAuthorityWithoutChangingStoredRole() {
  var users=mock(UserRepository.class); var functions=mock(FunctionPermissionRepository.class);
  var user=new User(); user.setId(43L); user.setUsername("member"); user.setRoles(List.of(Role.builder().name("USER").build()));
  when(users.findByUsernameOrEmailAndStatus("member","ACT")).thenReturn(Optional.of(user));
  when(functions.hasActiveStaffGroup(43L)).thenReturn(true);
  var service=new com.example.eshop.auth.security.UserDetailsService(users,functions);
  assertThat(service.loadUserByUsername("member").getAuthorities()).extracting("authority").contains("ADMIN","USER");
  assertThat(user.getRoles()).extracting(Role::getName).containsExactly("USER");
  when(functions.hasActiveStaffGroup(43L)).thenReturn(false);
  assertThat(service.loadUserByUsername("member").getAuthorities()).extracting("authority").containsExactly("USER");
 }
}
