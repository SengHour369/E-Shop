package com.example.eshop.auth.service;

import com.example.eshop.auth.model.Role;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAccessServiceTest {
    final FunctionPermissionRepository functions = mock(FunctionPermissionRepository.class);
    final AccountAccessService service = new AccountAccessService(functions);
    User user(String role) {
        User user = new User(); user.setId(7L); user.setEnabled(true); user.setDeleted(false); user.setStatus("ACT");
        user.setRoles(List.of(Role.builder().name(role).build())); return user;
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorsReceiveEveryActiveFunctionWithoutDirectGrants(String role) {
        when(functions.activeCodes()).thenReturn(Set.of("ORDER_VIEW_ALL", "IMPORT_UPLOAD", "IMPORT_APPROVE", "IMPORT_EXECUTE", "FUTURE_FUNCTION"));
        var snapshot = service.snapshot(user(role));
        assertThat(snapshot.administrator()).isTrue(); assertThat(snapshot.permissions()).containsExactlyInAnyOrderElementsOf(functions.activeCodes());
        verify(functions,never()).effectiveCodes(anyLong());
    }
    @ParameterizedTest @ValueSource(strings = {"MANAGER", "CUSTOMER"})
    void otherRolesKeepExplicitGrants(String role) {
        when(functions.effectiveCodes(7L)).thenReturn(Set.of("PRODUCT_VIEW"));
        var snapshot = service.snapshot(user(role));
        assertThat(snapshot.administrator()).isFalse(); assertThat(snapshot.permissions()).containsExactly("PRODUCT_VIEW");
        verify(functions,never()).activeCodes();
    }
    @Test void roleRevocationTakesEffectOnNextResolution() {
        var user = user("ADMIN"); when(functions.activeCodes()).thenReturn(Set.of("ORDER_VIEW_ALL"));
        when(functions.effectiveCodes(7L)).thenReturn(Set.of("PRODUCT_VIEW"));
        assertThat(service.snapshot(user).permits("ORDER_VIEW_ALL")).isTrue();
        user.setRoles(List.of(Role.builder().name("CUSTOMER").build()));
        assertThat(service.snapshot(user).permits("ORDER_VIEW_ALL")).isFalse();
    }
    @Test void disabledDeletedAndSuspendedAdminsFailClosed() {
        var disabled = user("ADMIN"); disabled.setEnabled(false);
        var deleted = user("SUPER_ADMIN"); deleted.setDeleted(true);
        var suspended = user("ADMIN"); suspended.setStatus("SUS");
        for (User user : List.of(disabled,deleted,suspended)) assertThatThrownBy(() -> service.snapshot(user)).hasMessageContaining("Account unavailable");
        verifyNoInteractions(functions);
    }
}
