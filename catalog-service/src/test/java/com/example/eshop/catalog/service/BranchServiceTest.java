package com.example.eshop.catalog.service;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.client.BranchIdentityClient;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class BranchServiceTest {
 BranchRepository branches=mock(BranchRepository.class);BranchAccessRepository access=mock(BranchAccessRepository.class);InventoryRepository inventory=mock(InventoryRepository.class);LivePermissionService permissions=mock(LivePermissionService.class);AuditLogService audit=mock(AuditLogService.class);BranchIdentityClient identities=mock(BranchIdentityClient.class);
 BranchService service=new BranchService(branches,access,inventory,permissions,audit,identities);
 PermissionSnapshot staff=new PermissionSnapshot(7,false,Set.of("BRANCH_VIEW","BRANCH_CREATE","INVENTORY_ADJUST"));
 @BeforeEach void setup(){var actor=new UsernamePasswordAuthenticationToken("staff",null,List.of());actor.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(actor);when(permissions.current()).thenReturn(staff);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();RequestContextHolder.resetRequestAttributes();}
 Branch branch(){Branch b=new Branch();b.setId(3L);b.setName("Central");when(branches.findById(3L)).thenReturn(Optional.of(b));return b;}
 @Test void enforcesExplicitMembershipForStoredBranch(){branch();assertThatThrownBy(()->service.scope(3L)).hasMessageContaining("Branch access");when(access.existsByBranchIdAndUserIdAndActiveTrue(3L,7L)).thenReturn(true);service.scope(3L);when(access.existsByBranchIdAndUserIdAndActiveTrue(3L,7L)).thenReturn(false);assertThatThrownBy(()->service.scope(3L)).hasMessageContaining("Branch access");}
 @Test void rejectsUnknownAndInactiveBranches(){assertThatThrownBy(()->service.scope(9L)).hasMessageContaining("UNKNOWN_BRANCH");branch().setActive(false);assertThatThrownBy(()->service.scope(3L)).hasMessageContaining("UNKNOWN_BRANCH");}
 @Test void doesNotInferBranchForLegacyGlobalInventory(){service.scope(null);verifyNoInteractions(branches,access);}
 @Test void staffCannotAdministerMembershipOrInventoryAssociation(){assertThatThrownBy(()->service.grant(3,8,true)).hasMessageContaining("administrator");assertThatThrownBy(()->service.assignInventory(3,1)).hasMessageContaining("administrator");verifyNoInteractions(identities);}
 @Test void administratorsVerifyTargetIdentityAndAuditGrant(){when(permissions.current()).thenReturn(new PermissionSnapshot(7,true,Set.of("BRANCH_ACCESS_MANAGE")));when(branches.existsById(3L)).thenReturn(true);var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer test");RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));when(identities.active("Bearer test",8)).thenReturn(true);var grant=service.grant(3,8,true);assertThat(grant.getBranchId()).isEqualTo(3);assertThat(grant.getUserId()).isEqualTo(8);assertThat(grant.getUpdatedBy()).isEqualTo(7);verify(audit).record(any());}
 @Test void unknownUserFailsClosed(){when(permissions.current()).thenReturn(new PermissionSnapshot(7,true,Set.of("BRANCH_ACCESS_MANAGE")));when(branches.existsById(3L)).thenReturn(true);var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer test");RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));assertThatThrownBy(()->service.grant(3,8,true)).hasMessageContaining("Active user");verify(access,never()).saveAndFlush(any());}
 @Test void branchCreationDoesNotImplicitlyGrantAccess(){when(branches.saveAndFlush(any())).thenAnswer(c->{Branch b=c.getArgument(0);b.setId(1L);return b;});var b=service.create(" Central ","Address");assertThat(b.getNameKey()).isEqualTo("central");verifyNoInteractions(access);verify(audit).record(any());}
}
