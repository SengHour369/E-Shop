package com.example.eshop.auth.keycloak;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ExternalLoginAccountTest {
    @Mock JwtService jwt;
    @InjectMocks AuthServiceImpl auth;
    @Test void disabledDeletedOrBlockedLocalAccountCannotReceiveSession() {
        for (User user : new User[] {
            User.builder().enabled(false).deleted(false).status("ACT").build(),
            User.builder().enabled(true).deleted(true).status("ACT").build(),
            User.builder().enabled(true).deleted(false).status("BLK").build()
        }) {
            assertThatThrownBy(() -> auth.authenticateExternal(user))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        verifyNoInteractions(jwt);
    }
}
