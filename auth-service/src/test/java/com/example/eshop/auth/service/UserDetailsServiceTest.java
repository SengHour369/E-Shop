package com.example.eshop.auth.service;
import com.example.eshop.auth.service.impl.UserDetailsService;
import com.example.eshop.auth.service.impl.UserEditorService;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.common.audit.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.time.*;
class UserDetailsServiceTest {
 final UserEditorService editor=mock(UserEditorService.class);final UserRepository users=mock(UserRepository.class);
 final GroupRepository groups=mock(GroupRepository.class);final FunctionPermissionRepository functions=mock(FunctionPermissionRepository.class);
 final RefreshTokenRepository sessions=mock(RefreshTokenRepository.class);final AuditLogRepository audit=mock(AuditLogRepository.class);
 final UserDetailsService service=new UserDetailsService(editor,users,groups,functions,sessions,audit);
 final User user=User.builder().id(7L).username("john").fullName("John Doe").status("ACT").enabled(true).roles(List.of(Role.builder().name("ADMIN").build())).build();
 @BeforeEach void setup(){
  when(users.findUserById(7L)).thenReturn(Optional.of(user));when(editor.get(7L)).thenReturn(Map.of("id",7L,"groupIds",List.of()));
  when(groups.findAllById(any())).thenReturn(List.of());when(functions.activeCodes()).thenReturn(Set.of("PRODUCT_VIEW"));
  when(functions.findAll()).thenReturn(List.of(FunctionPermission.builder().funcCode("PRODUCT_VIEW").funcName("View Products").build()));
  when(audit.findAll(any(Specification.class),any(Pageable.class))).thenReturn(Page.empty());
  when(sessions.findByUser_IdAndExpiresAtAfterOrderByCreatedAtDesc(eq(7L),any())).thenReturn(List.of(RefreshToken.builder().id(UUID.randomUUID()).token("secret-hash-must-not-leak").createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1)).build()));
 }
 @Test void returnsRealSecurityMetadataWithoutSessionTokens(){
  var result=service.get(7L);assertThat(result).containsEntry("activeSessions",1).containsEntry("accessLevel","Administrator");
  assertThat(result.get("lastLogin")).isNull();assertThat(result.get("passwordChangedAt")).isNull();
  assertThat(result.toString()).doesNotContain("secret-hash-must-not-leak").doesNotContain("password=");
  assertThat((List<?>)result.get("permissions")).hasSize(1);
 }
 @Test void blockedUsersRetainProfileButHaveNoActiveSessions(){
  user.setStatus("BLK");var result=service.get(7L);assertThat(result).containsEntry("activeSessions",0).containsEntry("accessLevel","Account access blocked");verifyNoInteractions(sessions);
 }
 @Test void activityRejectsNegativePages(){assertThatThrownBy(()->service.activity(7L,-1)).hasMessageContaining("zero or greater");verifyNoInteractions(audit);}
 @Test void activityReturnsSanitizedEventsAndPaging(){
  var event=mock(AuditLog.class);when(event.getId()).thenReturn(1L);when(event.getActorId()).thenReturn("7");when(event.getAction()).thenReturn(AuditAction.LOGIN);when(event.getResult()).thenReturn(AuditResult.SUCCESS);when(event.getResourceType()).thenReturn("USER");when(event.getResourceId()).thenReturn("7");when(event.getOccurredAt()).thenReturn(Instant.now());
  when(audit.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event),PageRequest.of(0,20),1));when(users.findAllById(any())).thenReturn(List.of(user));
  var result=service.activity(7L,0);assertThat(result).containsEntry("totalElements",1L).containsEntry("totalPages",1);assertThat(result.toString()).contains("actorName=John Doe").doesNotContain("oldValue").doesNotContain("newValue");
 }
}
