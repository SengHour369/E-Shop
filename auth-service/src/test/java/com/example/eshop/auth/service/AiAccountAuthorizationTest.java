package com.example.eshop.auth.service;

import com.example.eshop.auth.controller.InternalAuthorizationController;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.FunctionPermissionRepository;
import com.example.eshop.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiAccountAuthorizationTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveAccountCannotUseAnOtherwiseValidIdentity() {
        var authentication = new UsernamePasswordAuthenticationToken("user-7", null, List.of());
        authentication.setDetails(Map.of("userId", 7L));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var users = mock(UserRepository.class);
        var functions = mock(FunctionPermissionRepository.class);
        var user = new User();
        user.setId(7L);
        user.setUsername("user-7");
        user.setEnabled(true);
        user.setDeleted(false);
        user.setStatus("INA");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        var controller = new InternalAuthorizationController(users, functions);
        assertThatThrownBy(() -> controller.current(authentication))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(functions);
    }
}
