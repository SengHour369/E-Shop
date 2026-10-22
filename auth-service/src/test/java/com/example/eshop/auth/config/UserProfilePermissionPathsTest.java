package com.example.eshop.auth.config;

import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.util.DataInitializer;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.AntPathMatcher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfilePermissionPathsTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void restartingAnExistingInstallationDoesNotRestoreOrExpandGroupGrants() {
        var roles = mock(RoleRepository.class); var users = mock(UserRepository.class);
        var groups = mock(GroupRepository.class); var functions = mock(FunctionPermissionRepository.class);
        var grants = mock(GroupPermissionRepository.class); var memberships = mock(UserGroupRepository.class);
        when(groups.count()).thenReturn(1L);
        when(groups.existsByGroupCode(anyString())).thenReturn(true);
        when(roles.existsByName(anyString())).thenReturn(true);
        when(users.existsByUsernameAndDeletedFalse("admin")).thenReturn(true);
        when(functions.existsById(anyLong())).thenReturn(true);
        var initializer = new DataInitializer(roles, users, null, groups, functions, mock(ApiPermissionRepository.class), grants, memberships);
        initializer.run();
        verifyNoInteractions(grants);
    }
    @Test void seedsMissingAdminRoutesWithDistinctViewAndActionPermissions() {
        var apis = mock(ApiPermissionRepository.class);
        var functions = mock(FunctionPermissionRepository.class);
        when(functions.findByFuncCodeAndIsDeleteFalse(anyString())).thenAnswer(call -> {
            String code = call.getArgument(0);
            return Optional.of(FunctionPermission.builder().funcId((long) code.hashCode()).funcCode(code).isActive(true).build());
        });
        var initializer = new DataInitializer(null, null, null, null, functions, apis, null, null);
        ReflectionTestUtils.invokeMethod(initializer, "seedApiPermissions");
        var saved = ArgumentCaptor.forClass(ApiPermission.class);
        verify(apis, atLeastOnce()).save(saved.capture());
        for (var expected : List.of(
            new String[]{"GET", "/api/v1/inventory/summary", "INVENTORY_VIEW"},
            new String[]{"POST", "/api/v1/inventory/delete", "INVENTORY_ADJUST"},
            new String[]{"POST", "/api/v1/refunds/list", "REFUND_VIEW"},
            new String[]{"POST", "/api/v1/refunds/*/process", "REFUND_PROCESS"},
            new String[]{"GET", "/api/v1/cancelations/summary", "CANCELATION_VIEW"},
            new String[]{"GET", "/api/v1/admin/promotions", "PROMOTION_VIEW"},
            new String[]{"DELETE", "/api/v1/admin/promotions/*", "PROMOTION_MANAGE"},
            new String[]{"GET", "/api/v1/orders/reports/monthly", "REPORT_VIEW"},
            new String[]{"POST", "/api/v1/products/delete/", "PRODUCT_DELETE"},
            new String[]{"POST", "/api/v1/products/update/status/", "PRODUCT_UPDATE"})) {
            assertThat(saved.getAllValues()).anySatisfy(api -> {
                assertThat(api.getMethod()).isEqualTo(expected[0]);
                assertThat(api.getApi()).isEqualTo(expected[1]);
                assertThat(api.getFuncId()).isEqualTo((long) expected[2].hashCode());
            });
        }
    }
    @Test void seededProfilePathsAllowGroupManagementGrantsAndRejectViewOnlyProductGrants() {
        var apis = mock(ApiPermissionRepository.class);
        var initializer = new DataInitializer(null, null, null, null, mock(FunctionPermissionRepository.class), apis, null, null);
        ReflectionTestUtils.invokeMethod(initializer, "seedApiPermissions");
        var saved = ArgumentCaptor.forClass(ApiPermission.class);
        verify(apis, atLeastOnce()).save(saved.capture());
        var mappings = saved.getAllValues();
        var users = mock(UserRepository.class);
        var functions = mock(FunctionPermissionRepository.class);
        var user = User.builder().id(43L).username("group-member")
            .roles(List.of(Role.builder().name("USER").build())).build();
        when(users.findByUsername("group-member")).thenReturn(Optional.of(user));
        when(functions.findById(601L)).thenReturn(Optional.of(FunctionPermission.builder().funcId(601L).funcCode("USER_MANAGE").build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "group-member", null, List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("GROUP_PERMISSION_SCOPED"))));
        var interceptor = new ApiPermissionInterceptor(apis, functions, null, users, null, null);
        var matcher = new AntPathMatcher();
        for (var endpoint : List.of(
            new String[]{"GET", "/api/v1/user/1/details"},
            new String[]{"GET", "/api/v1/user/1/details/activity"},
            new String[]{"GET", "/api/v1/user/editor-options"},
            new String[]{"GET", "/api/v1/user/1/editor"},
            new String[]{"PUT", "/api/v1/user/1/editor"},
            new String[]{"POST", "/api/v1/user/1/editor/photo"})) {
            var candidates = mappings.stream().filter(p -> p.getMethod().equals(endpoint[0]) && matcher.match(p.getApi(), endpoint[1])).toList();
            assertThat(candidates).as(Arrays.toString(endpoint)).hasSize(1);
            assertThat(candidates.get(0).getFuncId()).isEqualTo(601L);
            when(apis.findActiveByMethod(endpoint[0])).thenReturn(candidates);
            var request = new MockHttpServletRequest(endpoint[0], endpoint[1]);
            when(functions.effectiveCodes(43L)).thenReturn(Set.of("USER_MANAGE"));
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
            when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_VIEW"));
            assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .hasMessageContaining("permission");
        }
    }
}
