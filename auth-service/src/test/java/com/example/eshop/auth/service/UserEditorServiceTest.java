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
 final UserEditorService service=new UserEditorService(users,roles,groups,memberships,refresh,audit);
 final User user=User.builder().id(7L).username("john").email("john@example.test").phone("+15550000001").fullName("John Doe").roles(List.of(Role.builder().id(1L).name("CUSTOMER").build())).status("ACT").build();
 Update body(Long manager){return new Update("John","Doe","john@example.test","john","+15550000001","1990-05-15","Sales Manager","Sales",manager,"MALE","123 Main Street","en-US",true,List.of(1L),List.of(3L),true);}
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
}
