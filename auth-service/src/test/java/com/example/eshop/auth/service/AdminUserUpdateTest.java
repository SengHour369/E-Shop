package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.UserRequest;
import com.example.eshop.auth.exception.BadRequestException;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.service.impl.UserServiceImpl;
import com.example.eshop.common.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserUpdateTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock GroupRepository groupRepository;
    @Mock UserGroupRepository userGroupRepository;
    @Mock FunctionPermissionRepository functionPermissionRepository;
    @Mock UserPermissionRepository userPermissionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditLogService audit;
    @InjectMocks UserServiceImpl service;

    @Test void profileEditPreservesPasswordAndPersistsPhoneAndStatus() {
        User user = User.builder().id(7L).password("existing-hash").status("ACT").email("old@example.test").build();
        when(userRepository.findUserById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        service.updateUser(7L, UserRequest.builder().fullName("Jane Smith").email("new@example.test").phoneNumber("+85512345678").status("BLK").build());
        assertThat(user.getPassword()).isEqualTo("existing-hash");
        assertThat(user.getPhone()).isEqualTo("+85512345678");
        assertThat(user.getStatus()).isEqualTo("BLK");
        verifyNoInteractions(passwordEncoder);
    }
    @Test void rejectsInvalidStatusBeforeMutatingTheUser() {
        User user = User.builder().id(7L).status("ACT").fullName("Original").build();
        when(userRepository.findUserById(7L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.updateUser(7L, UserRequest.builder().fullName("Changed").status("INVALID").build())).isInstanceOf(BadRequestException.class);
        assertThat(user.getFullName()).isEqualTo("Original");
        verify(userRepository, never()).save(any());
    }
}
