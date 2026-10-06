package com.example.eshop.auth.service;

import com.example.eshop.auth.constant.Constant;
import com.example.eshop.auth.dto.request.RefreshTokenRequest;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.RefreshTokenRepository;
import com.example.eshop.auth.security.RefreshTokenHash;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RefreshCookieStorageTest {
    @Mock RefreshTokenRepository repository;
    @Mock JwtService jwt;
    @InjectMocks AuthServiceImpl auth;

    @Test void rotationLooksUpHashAndStoresOnlyNewHashLinkedToUser() {
        var user = User.builder().id(1L).username("shopper").password("encoded")
                .enabled(true).status(Constant.ACT).roles(List.of()).build();
        var old = RefreshToken.builder().token(RefreshTokenHash.of("old-cookie"))
                .user(user).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(repository.findByToken(RefreshTokenHash.of("old-cookie"))).thenReturn(Optional.of(old));
        when(jwt.generateToken(any())).thenReturn("access");
        var request = new RefreshTokenRequest();
        request.setRefreshToken("old-cookie");
        var result = auth.refreshToken(request);
        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        verify(repository).delete(old);
        assertThat(saved.getValue().getToken()).isEqualTo(RefreshTokenHash.of(result.refreshToken()))
                .isNotEqualTo(result.refreshToken());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test void hashCannotBePresentedAsTheCookieCredential() {
        var hash = RefreshTokenHash.of("cookie");
        assertThat(RefreshTokenHash.of(hash)).isNotEqualTo(hash);
        assertThat(RefreshTokenHash.of("abc")).isEqualTo(
                "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
