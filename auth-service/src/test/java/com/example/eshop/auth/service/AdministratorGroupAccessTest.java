package com.example.eshop.auth.service;

import com.example.eshop.auth.model.Role;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdministratorGroupAccessTest {
    final FunctionPermissionRepository functions = mock(FunctionPermissionRepository.class);
    final AccountAccessService access = new AccountAccessService(functions);
    User member() {
        var u = new User(); u.setId(43L); u.setUsername("member"); u.setEnabled(true);
        u.setStatus("ACT"); u.setDeleted(false); u.setRoles(List.of(Role.builder().name("USER").build()));
        return u;
    }
    @Test void administratorGroupGetsEntryButOnlyItsEffectiveFunctions() {
        when(functions.hasActiveStaffGroup(43L)).thenReturn(true);
        when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_VIEW"));
        var result = access.snapshot(member());
        assertThat(result.administrator()).isTrue();
        assertThat(result.permissions()).containsExactly("PRODUCT_VIEW");
        assertThat(result.permits("USER_MANAGE")).isFalse();
        verify(functions, never()).activeCodes();
    }
    @Test void removingMembershipRevokesAdministratorEntryOnNextRequest() {
        var user=member();
        when(functions.hasActiveStaffGroup(43L)).thenReturn(true, false);
        when(functions.effectiveCodes(43L)).thenReturn(Set.of("PRODUCT_VIEW"), Set.of());
        assertThat(access.snapshot(user).administrator()).isTrue();
        var revoked=access.snapshot(user);
        assertThat(revoked.administrator()).isFalse();
        assertThat(revoked.permissions()).isEmpty();
    }
}
