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
        var mvc = MockMvcBuilders.standaloneSetup(new InternalAuthorizationController(users, functions)).build();
        mvc.perform(get("/api/v1/user/access").param("userId", "99").principal(authentication))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(7))
            .andExpect(jsonPath("$.permissions[0]").value("PRODUCT_VIEW"));
        verify(functions).effectiveCodes(7L);
        verify(users, never()).findById(99L);
    }
}
