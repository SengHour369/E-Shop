package com.example.eshop.auth.service;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.common.security.PermissionSnapshot;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class UserApprovalRoutingTest {
 final UserRepository users=mock(UserRepository.class);final AccountAccessService access=mock(AccountAccessService.class);
 final UserApprovalRouting routing=new UserApprovalRouting(users,access);
 final User actor=User.builder().id(1L).username("maker").enabled(true).status("ACT").approvalReviewerId(2L).build();
 final User reviewer=User.builder().id(2L).username("reviewer").fullName("Same Name").enabled(true).status("ACT").build();
 @BeforeEach void setup(){var auth=new UsernamePasswordAuthenticationToken("maker","",List.of());auth.setDetails(Map.of("userId",1L));SecurityContextHolder.getContext().setAuthentication(auth);when(users.findById(1L)).thenReturn(Optional.of(actor));when(users.findAllUser()).thenReturn(List.of(actor,reviewer));when(access.snapshot(actor)).thenReturn(new PermissionSnapshot(1,false,Set.of("IMPORT_UPLOAD")));when(access.snapshot(reviewer)).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW","IMPORT_APPROVE","IMPORT_EXECUTE")));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void returnsActualNamesAndDefaultWithoutGrantingPermission(){var result=routing.currentOptions();assertThat(result).containsEntry("defaultReviewerId",2L);assertThat((List<?>)result.get("reviewers")).hasSize(1);assertThat(routing.candidates().get(0)).containsEntry("username","reviewer").containsEntry("name","Same Name");}
 @Test void filtersRevokedDisabledDeletedAndSelfAssignments(){
  when(access.snapshot(reviewer)).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW")));assertThat(routing.currentOptions().get("defaultReviewerId")).isNull();
  reviewer.setEnabled(false);assertThat(routing.eligible(reviewer)).isFalse();reviewer.setEnabled(true);reviewer.setDeleted(true);assertThat(routing.eligible(reviewer)).isFalse();
 }
 @Test void requiresCurrentUploadPermission(){when(access.snapshot(actor)).thenReturn(new PermissionSnapshot(1,false,Set.of()));assertThatThrownBy(()->routing.currentOptions()).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);}
}
