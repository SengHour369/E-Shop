package com.example.eshop.auth.controller;

import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import com.example.eshop.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CurrentAccessControllerTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void publicAccessEndpointReturnsOnlyTheCurrentActorsEffectiveCodes() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("reviewer", null, List.of());
        authentication.setDetails(Map.of("userId", 7L));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var users = mock(UserRepository.class);
        var functions = mock(FunctionPermissionRepository.class);
        var user = new User();
        user.setId(7L); user.setUsername("reviewer"); user.setEnabled(true);
        user.setDeleted(false); user.setStatus("ACT");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(functions.effectiveCodes(7L)).thenReturn(Set.of("PRODUCT_VIEW"));
        var mvc = MockMvcBuilders.standaloneSetup(new InternalAuthorizationController(users, new com.example.eshop.auth.service.AccountAccessService(functions), org.mockito.Mockito.mock(com.example.eshop.auth.repository.WorkflowGrantRepository.class))).build();
        mvc.perform(get("/api/v1/user/access").param("userId", "99").principal(authentication))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(7))
            .andExpect(jsonPath("$.permissions[0]").value("PRODUCT_VIEW"));
        verify(functions).effectiveCodes(7L);
        verify(users, never()).findById(99L);
    }
    @Test void administratorAccessUsesDatabaseRoleAndAllActiveCodes() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("administrator",null,List.of());
        authentication.setDetails(Map.of("userId",7L)); SecurityContextHolder.getContext().setAuthentication(authentication);
        var users=mock(UserRepository.class); var functions=mock(FunctionPermissionRepository.class);
        var user=new User(); user.setId(7L); user.setUsername("administrator"); user.setEnabled(true); user.setStatus("ACT"); user.setDeleted(false);
        user.setRoles(List.of(com.example.eshop.auth.model.Role.builder().name("ADMIN").build()));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(functions.activeCodes()).thenReturn(Set.of("ORDER_VIEW_ALL","IMPORT_UPLOAD","USER_VIEW"));
        var mvc=MockMvcBuilders.standaloneSetup(new InternalAuthorizationController(users,new com.example.eshop.auth.service.AccountAccessService(functions),org.mockito.Mockito.mock(com.example.eshop.auth.repository.WorkflowGrantRepository.class))).build();
        mvc.perform(get("/api/v1/user/access").principal(authentication)).andExpect(status().isOk())
            .andExpect(jsonPath("$.administrator").value(true))
            .andExpect(jsonPath("$.permissions",org.hamcrest.Matchers.containsInAnyOrder("ORDER_VIEW_ALL","IMPORT_UPLOAD","USER_VIEW")));
        verify(functions).activeCodes(); verify(functions,never()).effectiveCodes(anyLong());
    }
}
