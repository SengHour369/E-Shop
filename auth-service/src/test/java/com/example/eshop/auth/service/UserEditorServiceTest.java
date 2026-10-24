package com.example.eshop.auth.service;
import com.example.eshop.auth.controller.UserEditorController.Update;
import com.example.eshop.auth.service.impl.UserEditorService;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.common.audit.AuditLogService;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
class UserEditorServiceTest {
 final UserRepository users=mock(UserRepository.class);final RoleRepository roles=mock(RoleRepository.class);
 final GroupRepository groups=mock(GroupRepository.class);final UserGroupRepository memberships=mock(UserGroupRepository.class);
 final RefreshTokenRepository refresh=mock(RefreshTokenRepository.class);final AuditLogService audit=mock(AuditLogService.class);
 final UserPermissionRepository userPermissions=mock(UserPermissionRepository.class);final FunctionPermissionRepository functions=mock(FunctionPermissionRepository.class);
 final UserApprovalRouting approvals=mock(UserApprovalRouting.class);
 final UserEditorService service=new UserEditorService(users,roles,groups,memberships,refresh,userPermissions,functions,audit,approvals);
 final User user=User.builder().id(7L).username("john").email("john@example.test").phone("+15550000001").fullName("John Doe").roles(List.of(Role.builder().id(1L).name("CUSTOMER").build())).status("ACT").build();
 Update body(Long manager){return body(manager,null);}
 Update body(Long manager,Long reviewer){return body(manager,reviewer,null);}
 Update body(Long manager,Long reviewer,List<Long> permissions){return new Update("John","Doe","john@example.test","john","+15550000001","1990-05-15","Sales Manager","Sales",manager,"MALE","123 Main Street","en-US",true,List.of(1L),List.of(3L),true,reviewer,permissions);}
 @BeforeEach void setup(){when(users.findUserById(7L)).thenReturn(Optional.of(user));when(users.findAllUser()).thenReturn(List.of(user));when(roles.findAllById(any())).thenReturn(new ArrayList<>(user.getRoles()));when(groups.findAllById(any())).thenReturn(List.of(Group.builder().id(3L).isActive(true).isDelete(false).build()));when(memberships.findByUserId(7L)).thenReturn(List.of());when(memberships.findByUserIdAndIsDeleteFalse(7L)).thenReturn(List.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin",null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void savesProfileAndAssignmentsAndRevokesRefreshTokens(){var result=service.update(7L,body(null));assertThat(result).containsEntry("jobTitle","Sales Manager").containsEntry("gender","MALE").containsEntry("twoFactorRequired",true);verify(users).save(user);verify(memberships).save(argThat(m->m.getUserId().equals(7L)&&m.getGroupId().equals(3L)&&m.getIsActive()));verify(refresh).deleteByUser(user);assertThat(result).doesNotContainKey("password");}
 @Test void rejectsUnsupportedPhotoWithoutUpdatingUser(){
  var file=new org.springframework.mock.web.MockMultipartFile("file","bad.svg","image/svg+xml","<svg/>".getBytes());
  assertThatThrownBy(()->service.photo(7L,file)).hasMessageContaining("JPEG or PNG");verify(users,never()).save(any());
 }
 @Test void persistsPhotoAsSanitizedThumbnail() throws Exception {
  var image=new java.awt.image.BufferedImage(600,400,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
  service.photo(7L,new org.springframework.mock.web.MockMultipartFile("file","portrait.png","image/png",bytes.toByteArray()));
  var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(user.getProfileImage().split(",")[1])));
  assertThat(decoded.getWidth()).isEqualTo(256);assertThat(decoded.getHeight()).isEqualTo(170);verify(users).save(user);
 }
 @Test void rejectsSelfManagerBeforeWriting(){assertThatThrownBy(()->service.update(7L,body(7L))).hasMessageContaining("manage themselves");verify(users,never()).save(any());}
 @Test void rejectsDeletedGroupsBeforeWriting(){when(groups.findAllById(any())).thenReturn(List.of(Group.builder().id(3L).isActive(true).isDelete(true).build()));assertThatThrownBy(()->service.update(7L,body(null))).hasMessageContaining("active user groups");verify(users,never()).save(any());}
 @Test void rejectsDuplicateEmailBeforeWriting(){when(users.findAllUser()).thenReturn(List.of(user,User.builder().id(8L).username("someone").email("john@example.test").phone("+15550000002").build()));assertThatThrownBy(()->service.update(7L,body(null))).hasMessageContaining("already belongs");verify(users,never()).save(any());}
 @Test void persistsAnAuthorizedIndependentDefaultReviewer(){
  User reviewer=User.builder().id(8L).username("reviewer").status("ACT").enabled(true).build();
  when(users.findUserById(8L)).thenReturn(Optional.of(reviewer));when(approvals.eligible(reviewer)).thenReturn(true);
  assertThat(service.update(7L,body(null,8L))).containsEntry("approvalReviewerId",8L);assertThat(user.getApprovalReviewerId()).isEqualTo(8L);
 }
 @Test void rejectsSelfAndUnauthorizedReviewersBeforeSaving(){
  assertThatThrownBy(()->service.update(7L,body(null,7L))).hasMessageContaining("independent");
  User reviewer=User.builder().id(8L).build();when(users.findUserById(8L)).thenReturn(Optional.of(reviewer));
  assertThatThrownBy(()->service.update(7L,body(null,8L))).hasMessageContaining("permissions");verify(users,never()).save(any());
 }
 @Test void replacesDirectPermissionsAndRevokesSessionsWhenAccessChanges(){
  var previous=UserPermission.builder().userPermissionId(1L).userId(7L).funcId(11L).isActive(true).isDelete(false).build();
  var dormant=UserPermission.builder().userPermissionId(2L).userId(7L).funcId(12L).isActive(false).isDelete(true).build();
  when(userPermissions.findAllByUserId(7L)).thenReturn(List.of(previous,dormant));
  when(functions.findAllById(any())).thenReturn(List.of(FunctionPermission.builder().funcId(12L).funcCode("PRODUCT_VIEW").build()));
  service.update(7L,body(null,null,List.of(12L)));
  assertThat(previous.getIsActive()).isFalse();assertThat(previous.getIsDelete()).isTrue();
  assertThat(dormant.getIsActive()).isTrue();assertThat(dormant.getIsDelete()).isFalse();verify(refresh).deleteByUser(user);
 }
 @Test void rejectsInactiveOrUnknownPermissionIdsBeforeWriting(){
  when(functions.findAllById(any())).thenReturn(List.of(FunctionPermission.builder().funcId(12L).isActive(false).build()));
  assertThatThrownBy(()->service.update(7L,body(null,null,List.of(12L)))).hasMessageContaining("active user permissions");verify(users,never()).save(any());verify(userPermissions,never()).saveAll(any());
 }
 @Test void omittedPermissionsPreserveExistingAssignments(){service.update(7L,body(null));verify(userPermissions,never()).saveAll(any());}
}
