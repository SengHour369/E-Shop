package com.example.eshop.auth.service;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.auth.dto.request.Login;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.exception.CustomMessageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AuthAuditTest {
    @Mock UserRepository users;
    @Mock AuthenticationManager authenticationManager;
    @Mock AuditLogService audit;
    @InjectMocks AuthServiceImpl auth;
    @Test void rejectedLoginRecordsFailureWithoutClaimingClientIdentity() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));
        when(users.findByUsernameOrEmailAndStatus(anyString(), anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.authenticate(new Login("untrusted-user", "never-log-password")))
            .isInstanceOf(CustomMessageException.class);
        verify(audit).recordSecurity(AuditAction.LOGIN_FAILED, AuditResult.FAILURE, null, "AUTHENTICATION_FAILED");
        verifyNoMoreInteractions(audit);
    }
}
