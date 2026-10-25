package com.example.eshop.auth.controller;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.service.AccountAccessService;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.common.security.PermissionSnapshot;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class WorkflowGrantControllerTest {
 WorkflowGrantRepository grants=mock(WorkflowGrantRepository.class);UserRepository users=mock(UserRepository.class);AccountAccessService access=mock(AccountAccessService.class);JwtService jwt=mock(JwtService.class);WorkflowGrantController controller=new WorkflowGrantController(grants,users,access,jwt,mock(AuditLogService.class));
 @BeforeEach void identity(){var auth=new UsernamePasswordAuthenticationToken("owner",null,List.of());auth.setDetails(Map.of("userId",1L));SecurityContextHolder.getContext().setAuthentication(auth);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 WorkflowGrant grant(){var g=new WorkflowGrant();g.setId(UUID.randomUUID());g.setOwnerId(1);g.setVersionId(UUID.randomUUID());g.setExpiresAt(Instant.now().plusSeconds(3600));when(grants.findById(g.getId())).thenReturn(Optional.of(g));return g;}
 @Test void exchangeRejectsRevokedExpiredAndOtherVersions(){var g=grant();g.setRevoked(true);assertThatThrownBy(()->controller.exchange(new WorkflowGrantController.Exchange(g.getId(),g.getVersionId()))).hasMessageContaining("expired or revoked");g.setRevoked(false);g.setExpiresAt(Instant.now().minusSeconds(1));assertThatThrownBy(()->controller.exchange(new WorkflowGrantController.Exchange(g.getId(),g.getVersionId()))).hasMessageContaining("expired or revoked");g.setExpiresAt(Instant.now().plusSeconds(3600));assertThatThrownBy(()->controller.exchange(new WorkflowGrantController.Exchange(g.getId(),UUID.randomUUID()))).hasMessageContaining("expired or revoked");}
 @Test void grantsAreOwnedAndRevocable(){var g=grant();g.setOwnerId(2);assertThatThrownBy(()->controller.revoke(g.getId())).hasMessageContaining("404");assertThat(g.isRevoked()).isFalse();g.setOwnerId(1);controller.revoke(g.getId());assertThat(g.isRevoked()).isTrue();}
 @Test void exchangeCreatesShortLivedReadOnlyCredential(){var g=grant();var user=mock(User.class);when(user.getId()).thenReturn(1L);when(user.getUsername()).thenReturn("owner");when(users.findById(1L)).thenReturn(Optional.of(user));when(access.snapshot(user)).thenReturn(new PermissionSnapshot(1,true,Set.of()));var key=Keys.hmacShaKeyFor(new byte[64]);when(jwt.getKey()).thenReturn(key);var result=controller.exchange(new WorkflowGrantController.Exchange(g.getId(),g.getVersionId()));var claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(result.get("accessToken").toString()).getBody();assertThat(claims.get("authorities")).isEqualTo(List.of("WORKFLOW_READ"));assertThat(claims.get("workflowGrantId")).isEqualTo(g.getId().toString());assertThat(claims.getExpiration().toInstant()).isBefore(Instant.now().plusSeconds(601));}
}
